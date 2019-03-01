package net.n2oapp.framework.config.metadata.compile;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.StringUtils;
import net.n2oapp.framework.api.metadata.Compiled;
import net.n2oapp.framework.api.metadata.Source;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.aware.ExtensionAttributesAware;
import net.n2oapp.framework.api.metadata.aware.RefIdAware;
import net.n2oapp.framework.api.metadata.compile.BindProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.compile.ExtensionAttributeMapperFactory;
import net.n2oapp.framework.api.metadata.compile.building.Placeholders;
import net.n2oapp.framework.api.metadata.meta.BindLink;
import net.n2oapp.framework.api.metadata.meta.ModelLink;
import net.n2oapp.framework.api.metadata.meta.control.DefaultValues;
import net.n2oapp.framework.api.metadata.pipeline.*;
import net.n2oapp.framework.api.metadata.validate.ValidateProcessor;
import net.n2oapp.framework.api.metadata.validation.exception.N2oMetadataValidationException;
import net.n2oapp.framework.api.script.ScriptProcessor;
import net.n2oapp.framework.config.compile.pipeline.N2oPipelineSupport;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

import static net.n2oapp.framework.config.register.route.RouteUtil.getParams;

/**
 * Реализация процессора сборки метаданных
 */
public class N2oCompileProcessor implements CompileProcessor, BindProcessor, ValidateProcessor {

    private MetadataEnvironment env;
    private Map<Class<?>, Object> scope = Collections.emptyMap();
    private DataSet data;
    private BindTerminalPipeline bindPipeline;
    private CompileTerminalPipeline<?> compilePipeline;
    private ReadCompileTerminalPipeline<?> readCompilePipeline;
    private ReadTerminalPipeline<?> readPipeline;
    /**
     * Контекст на входе в pipeline, используется не для компиляции, а для bind
     */
    private CompileContext<?, ?> context;

    /**
     * Конструктор процессора сборки метаданных
     *
     * @param env Окружение сборки метаданных
     */
    public N2oCompileProcessor(MetadataEnvironment env) {
        this.env = env;
        PipelineSupport pipelineSupport = new N2oPipelineSupport(env);
        this.bindPipeline = env.getBindPipelineFunction().apply(pipelineSupport);
        this.readPipeline = env.getReadPipelineFunction().apply(pipelineSupport);
        this.readCompilePipeline = env.getReadCompilePipelineFunction().apply(pipelineSupport);
        this.compilePipeline = env.getCompilePipelineFunction().apply(pipelineSupport);
    }

    /**
     * Конструктор процессора сборки метаданных со связыванием
     *
     * @param env     Окружение сборки метаданных
     * @param data    Данные для связывания
     * @param context Входной контекст сборки(не используется для компиляции метаданных)
     */
    public N2oCompileProcessor(MetadataEnvironment env, CompileContext<?, ?> context, DataSet data) {
        this(env);
        this.data = data;
        this.context = context;
    }

    /**
     * Конструктор процессора внутренней сборки метаданных
     *
     * @param parent Родительский процессор сборки
     * @param scope  Метаданные, влияющие на сборку. Должны быть разных классов.
     */
    private N2oCompileProcessor(N2oCompileProcessor parent, Object... scope) {
        this.env = parent.env;
        this.scope = new HashMap<>(parent.scope);
        Stream.of(Optional.ofNullable(scope).orElse(new Compiled[]{})).filter(Objects::nonNull)
                .forEach(s -> this.scope.put(s.getClass(), s));
        this.readPipeline = parent.readPipeline;
        this.readCompilePipeline = parent.readCompilePipeline;
        this.compilePipeline = parent.compilePipeline;
        this.data = parent.data;
        this.context = parent.context;
    }

    @Override
    public <D extends Compiled, S> D compile(S source, CompileContext<?, ?> context, Object... scope) {
        return compilePipeline.get(source, context, new N2oCompileProcessor(this, scope));
    }

    @Override
    public <D extends Compiled> void bind(D compiled) {
        bindPipeline.get(compiled, context, data);
    }

    @Override
    public Map<String, Object> mapAttributes(ExtensionAttributesAware source) {
        if (source.getExtAttributes() == null)
            return null;
        ExtensionAttributeMapperFactory extensionAttributeMapperFactory = env.getExtensionAttributeMapperFactory();
        HashMap<String, Object> extAttributes = new HashMap<>();
        source.getExtAttributes().forEach((k, v) -> {
            Map<String, Object> res = extensionAttributeMapperFactory.mapAttributes(v, k.getUri());
            if (res != null) {
                extAttributes.putAll(res);
            }
        });
        return extAttributes;
    }

    @Override
    public <D extends Compiled> D getCompiled(CompileContext<D, ?> context) {
        return readCompilePipeline.get(context);
    }

    @Override
    public <D> D getScope(Class<D> scopeClass) {
        return (D) scope.get(scopeClass);
    }

    @Override
    public <S extends SourceMetadata> S getSource(String id, Class<S> sourceClass) {
        return readPipeline.get(id, sourceClass);
    }

    @Override
    public <D extends Compiled> void addRoute(CompileContext<D, ?> context) {
        env.getRouteRegister().addRoute(context.getRoute(this), context);
    }

    @Override
    public <D extends Compiled> void addRoute(String route, CompileContext<D, ?> context) {
        env.getRouteRegister().addRoute(route, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T resolve(String placeholder, Class<T> clazz) {
        Object value = placeholder;
        if (StringUtils.isProperty(placeholder)) {
            value = env.getSystemProperties().resolvePlaceholders(placeholder);
        }
        if (StringUtils.isContext(placeholder)) {
            value = env.getContextProcessor().resolve(placeholder);
        }
        return (T) env.getDomainProcessor().deserialize(value, clazz);
    }

    @Override
    public Object resolve(String value, String domain) {
        return env.getDomainProcessor().deserialize(value, domain);
    }

    @Override
    public Object resolve(String value) {
        return env.getDomainProcessor().deserialize(value);
    }

    @Override
    public String resolveText(String text) {
        if (StringUtils.hasProperty(text))
            return env.getSystemProperties().resolvePlaceholders(text);
        if (StringUtils.hasContext(text))
            return env.getContextProcessor().resolveText(text);
        return text;
    }

    @Override
    public String resolveUrl(String url) {
        return StringUtils.resolveLinks(url, data);
    }


    @Override
    public String getMessage(String messageCode, Object... arguments) {
        String defaultMessage = messageCode.contains("{0}") ? MessageFormat.format(messageCode, arguments) : messageCode;
        return env.getMessageSource().getMessage(messageCode, arguments, defaultMessage);
    }

    @Override
    public Object resolveJS(String text, Class<?> clazz) {
        String value = ScriptProcessor.resolveLinks(text);
        return env.getDomainProcessor().deserialize(value, clazz);
    }

    @Override
    public String resolveUrl(String url,
                             Map<String, ? extends BindLink> pathMappings,
                             Map<String, ? extends BindLink> queryMappings) {
        Set<String> params = new HashSet<>(getParams(url));
        Set<String> paramsForRemove = new HashSet<>();
        Set<String> except = new HashSet<>();
        if (pathMappings != null) {
            if (context.getPathRouteMapping() != null) {
                pathMappings.keySet().stream().filter(k -> !context.getPathRouteMapping().containsKey(k))
                        .forEach(except::add);
            } else {
                except.addAll(pathMappings.keySet());
            }
        }
        if (queryMappings != null) {
            if (context.getQueryRouteMapping() != null) {
                queryMappings.keySet().stream().filter(k -> !context.getQueryRouteMapping().containsKey(k))
                        .forEach(except::add);
            } else {
                except.addAll(queryMappings.keySet());
            }
        }
        for (String param : params) {
            if (!except.contains(param)) {
                Object value = data.get(param);
                if (value != null) {
                    url = url.replace(":" + param, value.toString());
                    paramsForRemove.add(param);
                }
            }
        }
        if (pathMappings != null) {
            paramsForRemove.forEach(pathMappings::remove);
        }
        if (queryMappings != null) {
            paramsForRemove.forEach(queryMappings::remove);
        }
        return url;
    }

    @Override
    public String resolveUrl(String url, ModelLink link) {
        List<String> params = getParams(url);
        if (params == null || params.isEmpty() || data == null)
            return url;
        Map<String, String> valueParamMap = new HashMap<>();
        collectModelLinks(context.getPathRouteMapping(), link, valueParamMap);
        collectModelLinks(context.getQueryRouteMapping(), link, valueParamMap);
        for (String param : params) {
            if (valueParamMap.containsKey(param) && data.containsKey(valueParamMap.get(param))) {
                url = url.replace(":" + param, data.get(valueParamMap.get(param)).toString());
            }
        }
        return url;
    }

    @Override
    public ModelLink resolveLink(ModelLink link) {
        if (link == null || link.getBindLink() == null || context == null || context.getQueryRouteMapping() == null)
            return link;
        Optional<String> res = Optional.empty();
        if (context.getQueryRouteMapping() != null) {
            res = context.getQueryRouteMapping().keySet().stream().filter(ri -> context.getQueryRouteMapping().get(ri).equals(link)).findAny();
        }
        if (!res.isPresent() && context.getPathRouteMapping() != null) {
            res = context.getPathRouteMapping().keySet().stream().filter(ri -> context.getPathRouteMapping().get(ri).equals(link)).findAny();
        }
        if (res.isPresent()) {
            Object value = data.get(res.get());
            if (value != null) {
                return new ModelLink(value);
            }
        }
        return link;
    }

    @Override
    public void resolveSubModels(ModelLink link, List<ModelLink> linkList) {
        if (link.getSubModelQuery() == null) return;
        for (ModelLink modelLink : linkList) {
            if (link.equalsLink(modelLink)) {
                resolveDefaultValues(modelLink, link);
            }
        }
        executeSubModels(link);
    }

    @Override
    public String resolveText(String text, ModelLink link) {
        Set<String> links = StringUtils.collectLinks(text);
        if (links == null || links.isEmpty() || data == null)
            return text;
        Map<String, String> valueParamMap = new HashMap<>();
        collectModelLinks(context.getPathRouteMapping(), link, valueParamMap);
        collectModelLinks(context.getQueryRouteMapping(), link, valueParamMap);
        for (String l : links) {
            if (valueParamMap.containsKey(l) && data.containsKey(valueParamMap.get(l))) {
                text = text.replace(Placeholders.ref(l), data.get(valueParamMap.get(l)).toString());
            }
        }
        return text;
    }

    @Override
    public <T extends Source> void validate(T metadata) {
        if (metadata == null)
            return;
        env.getSourceValidatorFactory().validate(metadata, this);
    }

    @Override
    public <T extends SourceMetadata> T getOrNull(String id, Class<T> metadataClass) {
        if (id == null)
            return null;
        if (!env.getMetadataRegister().contains(id, metadataClass))
            return null;
        try {
            return getSource(id, metadataClass);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T extends SourceMetadata> T getOrThrow(String id, Class<T> metadataClass) {
        if (id == null)
            return null;
        if (!env.getMetadataRegister().contains(id, metadataClass))
            return null;
        return getSource(id, metadataClass);
    }

    @Override
    public <T extends SourceMetadata> void checkForExists(String id, Class<T> metadataClass, String errorMessage) {
        if (id == null)
            return;
        if (!env.getMetadataRegister().contains(id, metadataClass))
            throw new N2oMetadataValidationException(getMessage(errorMessage, id));
    }


    private void collectModelLinks(Map<String, ModelLink> linkMap, ModelLink link, Map<String, String> resultMap) {
        if (linkMap != null) {
            linkMap.forEach((k, v) -> {
                if (v.equalsLink(link)) {
                    // для данных, которые мапятся напрямую
                    resultMap.put(k, k);
                    // для данных, которые мапятся через параметр
                    resultMap.put(v.getFieldId(), k);
                }
            });
        }
    }

    private void executeSubModels(ModelLink link) {
        if (link.getValue() == null)
            return;
        if (link.getValue() instanceof List) {
            for (DefaultValues defaultValues : (List<DefaultValues>) link.getValue()) {
                DataSet dataSet = new DataSet();
                dataSet.put(link.getFieldId(), defaultValues.getValues());
                env.getSubModelsProcessor().executeSubModels(Collections.singletonList(link.getSubModelQuery()), dataSet);
                defaultValues.setValues((Map<String, Object>) dataSet.get(link.getFieldId()));
            }
        } else if (link.getValue() instanceof DefaultValues) {
            DataSet dataSet = new DataSet();
            dataSet.put(link.getFieldId(), ((DefaultValues) link.getValue()).getValues());
            env.getSubModelsProcessor().executeSubModels(Collections.singletonList(link.getSubModelQuery()), dataSet);
            ((DefaultValues) link.getValue()).setValues((Map<String, Object>) dataSet.get(link.getFieldId()));
        }
    }

    private void resolveDefaultValues(ModelLink src, ModelLink dst) {
        if (src.getParam() != null && data.containsKey(src.getParam())) {
            if (data.get(src.getParam()) instanceof List) {
                List<DefaultValues> values = new ArrayList<>();
                for (Object value : (List) data.get(src.getParam())) {
                    DefaultValues defaultValues = new DefaultValues();
                    defaultValues.setValues(new HashMap<>());
                    defaultValues.getValues().put(src.getSubModelQuery().getValueFieldId(), value);
                    values.add(defaultValues);
                }
                if (!values.isEmpty())
                    dst.setValue(values);
            } else {
                DefaultValues defaultValues = new DefaultValues();
                defaultValues.setValues(new HashMap<>());
                defaultValues.getValues().put(src.getSubModelQuery().getValueFieldId(), data.get(src.getParam()));
                dst.setValue(src.getSubModelQuery().getMulti() != null && src.getSubModelQuery().getMulti()
                        ? Collections.singletonList(defaultValues)
                        : defaultValues);
            }
        }
    }
}
