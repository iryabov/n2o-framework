package net.n2oapp.framework.config.util;

import net.n2oapp.framework.api.exception.N2oException;
import net.n2oapp.framework.config.register.storage.Node;
import net.n2oapp.framework.config.register.storage.PathUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author operehod
 * @since 15.04.2015
 */
public class FileSystemUtil {

    private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new DefaultResourceLoader();


    @SuppressWarnings("unchecked")
    public static void saveContentToFile(InputStream content, File file) {
        try {
            File dir = file.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Can not touch file " + file.getAbsolutePath(), e);
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            int b;
            while ((b = content.read()) != -1) {
                bos.write(b);
            }
        } catch (IOException e) {
            throw new RuntimeException("Can not save content into file " + file.getAbsolutePath(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void saveContentToFile(String content, File file) {
        try (InputStream inputStream = IOUtils.toInputStream(content, "UTF-8")) {
            saveContentToFile(inputStream, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Boolean removeContentByUri(String uri) {
        Resource resource = DEFAULT_RESOURCE_LOADER.getResource(uri);
        File target;
        Boolean isDeleted = false;
        try {
            target = resource.getFile();
            isDeleted = target.delete();
        } catch (IOException e) {
            throw new RuntimeException("Can not delete file " + uri, e);
        }
        return isDeleted;
    }


    public static List<Node> getNodesByLocationPattern(String locationPattern) {
        return getNodesByLocationPattern(locationPattern, name -> true);
    }

    public static List<Node> getNodesByLocationPattern(List<String> locationPattern) {
        List<Node> nodes = new ArrayList<>();
        locationPattern.forEach(ptn -> nodes.addAll(getNodesByLocationPattern(ptn, name -> true)));
        return nodes;
    }

    public static final Predicate<String> FILE_NAME_WITHOUT_DOTS = s -> {
        int idx = s.lastIndexOf('.');
        return !s.substring(0, idx).contains(".");
    };

    public static Node getNodeByClasspathUri(String uri) {
        ClassPathResource resource = new ClassPathResource(uri);
        try {
            return Node.byLocationPattern(resource, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Node> getNodesByLocationPattern(String locationPattern, Predicate<String> nameFilter) {
        PathMatchingResourcePatternResolver pathPatternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = pathPatternResolver.getResources(locationPattern);
            List<Node> nodes = new ArrayList<>();
            for (Resource resource : resources) {
                if (!nameFilter.test(resource.getFilename()))
                    continue;
                nodes.add(Node.byLocationPattern(resource, locationPattern));
            }
            return nodes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getContent(Node node) {
        return getContentByUri(node.getURI());
    }

    public static String getContentByUri(String URI) {
        return getContentByUri(URI, true);
    }

    public static String getContentByUri(String URI, boolean isExistRequired) {
        try (InputStream inputStream = getContentAsStream(URI, isExistRequired)) {
            return inputStream == null ? null : IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getContentAsStream(String path) throws IOException {
        return getContentAsStream(path, true);
    }

    public static InputStream getContentAsStream(String path, boolean isExistRequired) throws IOException {
        if (path == null)
            return null;
        Resource resource = DEFAULT_RESOURCE_LOADER.getResource(path);
        if (!resource.exists()) {
            if (isExistRequired)
                throw new IllegalArgumentException("File '" + path + "' not found");
            else
                return null;
        }
        return resource.getInputStream();
    }

    public static String getContentFromResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeAllFromDirectory(String dir, List<String> excludePaths) throws IOException {
        File root = new File(dir);
        for (File file : root.listFiles()) {
            deleteRecursively(file, excludePaths);
        }
    }

    public static long getFileSizeByUri(String uri) throws IOException {
        if (uri == null)
            return 0;
        if (uri.startsWith("jar:") || uri.startsWith("classpath:")) {
            Resource resource = DEFAULT_RESOURCE_LOADER.getResource(uri);
            if (!resource.exists()) {
                throw new N2oException("File Not Found:"+uri);
            }
            return resource.contentLength();
        } else if (uri.startsWith("file:")) {
            File file = new File(PathUtil.convertUrlToAbsolutePath(uri));
            if (!file.exists()){
                throw new N2oException("File Not Found:"+uri);
            }
            return file.length();
        }
        return 0;
    }

    private static void deleteRecursively(File dir, List<String> excludePaths) throws IOException {
        if (!dir.exists() || excludePaths.contains(PathUtil.normalize(dir.getAbsolutePath())))
            return;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                IOException exception = null;
                for (File file : files) {
                    try {
                        deleteRecursively(file, excludePaths);
                    } catch (IOException ioe) {
                        exception = ioe;
                    }
                }
                if (null != exception) {
                    throw exception;
                }
            }
            if (dir.listFiles() == null || dir.listFiles().length == 0) {
                if (!dir.delete()) {
                    throw new IOException("Unable to delete directory: " + dir);
                }
            }
        } else {
            if (!dir.delete()) {
                throw new IOException("Unable to delete file: " + dir);
            }
        }
    }
}
