package cn.org.openygt.iot.gateway.plugin;

import cn.org.openygt.iot.adapter.DeviceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 外部插件 Bean 定义加载器
 *
 * <p>在 Spring 容器初始化早期扫描外部 JAR 文件，将其中实现了 {@link DeviceAdapter}
 * 接口且带有 {@link Component} 注解的类注册为 Spring Bean，使其能够参与依赖注入
 * 和生命周期管理。</p>
 */
@Slf4j
@Component
public class ExternalPluginBeanDefinitionLoader implements BeanDefinitionRegistryPostProcessor, PriorityOrdered, EnvironmentAware {

    private static final String PLUGIN_DIR_PROP = "iot.gateway.plugin-dir";
    private static final String DEFAULT_PLUGIN_DIR = "./plugins";

    private Environment environment;

    public ExternalPluginBeanDefinitionLoader() {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (environment == null) {
            log.warn("Environment 未设置，跳过外部插件加载");
            return;
        }

        String pluginDir = environment.getProperty(PLUGIN_DIR_PROP, DEFAULT_PLUGIN_DIR);
        File dir = new File(pluginDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.debug("插件目录不存在或不是目录: {}", pluginDir);
            return;
        }

        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.debug("插件目录中未找到 JAR 文件: {}", pluginDir);
            return;
        }

        List<URL> urls = new ArrayList<>();
        for (File jarFile : jarFiles) {
            try {
                urls.add(jarFile.toURI().toURL());
                log.info("发现插件 JAR: {}", jarFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("无法加载插件 JAR: {}", jarFile.getAbsolutePath(), e);
            }
        }

        if (urls.isEmpty()) {
            return;
        }

        URLClassLoader pluginClassLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader());

        try {
            CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(pluginClassLoader);

            for (URL url : urls) {
                scanJar(registry, url, metadataReaderFactory, pluginClassLoader);
            }
        } finally {
            // URLClassLoader 不关闭，因为 Bean 实例化还需要它
        }
    }

    private void scanJar(BeanDefinitionRegistry registry, URL jarUrl,
                         CachingMetadataReaderFactory metadataReaderFactory,
                         ClassLoader classLoader) {
        try {
            String jarPath = jarUrl.getPath();
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring(5);
            }

            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (!entryName.endsWith(".class")) {
                        continue;
                    }

                    String className = entryName.replace('/', '.').replace(".class", "");
                    try {
                        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
                        if (metadataReader.getAnnotationMetadata().hasAnnotation(Component.class.getName())) {
                            String[] interfaces = metadataReader.getClassMetadata().getInterfaceNames();
                            boolean isAdapter = false;
                            for (String iface : interfaces) {
                                if (DeviceAdapter.class.getName().equals(iface)) {
                                    isAdapter = true;
                                    break;
                                }
                            }
                            // 也检查父类是否实现了 DeviceAdapter
                            if (!isAdapter) {
                                Class<?> clazz = Class.forName(className, false, classLoader);
                                isAdapter = DeviceAdapter.class.isAssignableFrom(clazz);
                            }

                            if (isAdapter) {
                                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                                // 直接设置 Class 对象，避免 Spring 用主 ClassLoader 重新加载
                                Class<?> clazz = Class.forName(className, true, classLoader);
                                beanDefinition.setBeanClass(clazz);
                                beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
                                String beanName = generateBeanName(className);
                                registry.registerBeanDefinition(beanName, beanDefinition);
                                log.info("注册外部插件 Bean: {} -> {}", beanName, className);
                            }
                        }
                    } catch (NoClassDefFoundError e) {
                        log.debug("跳过类（依赖缺失）: {}", className);
                    } catch (Exception e) {
                        log.debug("无法读取类元数据: {}", className, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描 JAR 失败: {}", jarUrl, e);
        }
    }

    private String generateBeanName(String className) {
        int lastDot = className.lastIndexOf('.');
        String simpleName = lastDot > 0 ? className.substring(lastDot + 1) : className;
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 无需额外处理
    }
}
