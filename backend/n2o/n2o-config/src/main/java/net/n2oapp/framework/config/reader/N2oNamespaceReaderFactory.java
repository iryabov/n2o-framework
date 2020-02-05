package net.n2oapp.framework.config.reader;

import net.n2oapp.engine.factory.EngineNotFoundException;
import net.n2oapp.framework.api.metadata.aware.NamespaceUriAware;
import net.n2oapp.framework.api.metadata.aware.ReaderFactoryAware;
import net.n2oapp.framework.api.metadata.io.IOProcessor;
import net.n2oapp.framework.api.metadata.io.IOProcessorAware;
import net.n2oapp.framework.api.metadata.io.NamespaceIO;
import net.n2oapp.framework.api.metadata.io.ProxyNamespaceIO;
import net.n2oapp.framework.api.metadata.reader.NamespaceReader;
import net.n2oapp.framework.api.metadata.reader.NamespaceReaderFactory;
import org.jdom.Namespace;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;

/**
 * Фабрика, генерирующая сервисы чтения xml файлов(метаданных) в объекты n2o.
 * Подходящий сервис отыскивается сначала по имени DOM элемента, если он неуникальный поиск уточняется по namespaceUri.
 */
public class N2oNamespaceReaderFactory<T extends NamespaceUriAware> implements NamespaceReaderFactory<T, NamespaceReader<T>>,
        ApplicationContextAware, IOProcessorAware {

    private ApplicationContext applicationContext;
    private IOProcessor processor;
    // первый параметр - namespace, второй element
    private volatile Map<String, Map<String, NamespaceReader<T>>> engines;

    @Override
    public NamespaceReader<T> produce(String elementName, Namespace namespace) {
        if (engines == null)
            initFactory();
        Map<String, NamespaceReader<T>> elementReaders = engines.get(namespace.getURI());
        if (elementReaders == null)
            throw new EngineNotFoundException(namespace.getURI());
        NamespaceReader<T> reader = elementReaders.get(elementName);
        if (reader == null)
            throw new EngineNotFoundException(elementName);
        if (reader instanceof ReaderFactoryAware)
            ((ReaderFactoryAware) reader).setReaderFactory(this);
        if (reader instanceof IOProcessorAware)
            ((IOProcessorAware) reader).setIOProcessor(this.processor);
        return reader;
    }

    @Override
    public boolean check(Namespace namespace, String elementName) {
        if (engines == null)
            initFactory();

        Map<String, NamespaceReader<T>> elementReaders = engines.get(namespace.getURI());
        return elementReaders != null && elementReaders.containsKey(elementName);
    }

    private synchronized void initFactory() {
        if (engines == null) {
            Map<String, Map<String, NamespaceReader<T>>> result = new HashMap<>();
            Collection<NamespaceReader> beans = new ArrayList<>(applicationContext.getBeansOfType(NamespaceReader.class).values());
            if (processor != null) {
                for (NamespaceIO ioBean : applicationContext.getBeansOfType(NamespaceIO.class).values()) {
                    beans.add(new ProxyNamespaceIO<>(ioBean, processor));
                }
            }
            beans.forEach(b -> {
                String namespaceUri = b.getNamespaceUri();
                if (result.containsKey(namespaceUri)) {
                    result.get(namespaceUri).put(b.getElementName(), b);
                } else {
                    Map<String, NamespaceReader<T>> typedEngines = new HashMap<>();
                    typedEngines.put(b.getElementName(), b);
                    result.put(namespaceUri, typedEngines);
                }
            });
            engines = result;
        }
    }

    @Override
    public void add(NamespaceReader<T> reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setIOProcessor(IOProcessor processor) {
        this.processor = processor;
    }

}
