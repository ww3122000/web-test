package com.ksc.s3;

import java.lang.management.ManagementFactory;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class StartUp {
    

	public static void main(String[] args) throws Exception {
		SpringApplication.run(StartUp.class, args);
	}
	
	
//	@Bean
    public EmbeddedServletContainerFactory servletContainer() {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setPort(20020);
        factory.addServerCustomizers(new JettyServerCustomizer() {

            @Override
            public void customize(Server server) {
                ((QueuedThreadPool)server.getThreadPool()).setMaxThreads(10);
                
                MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
                server.addEventListener(mbContainer);
                server.addBean(mbContainer);
                
                int countx = 0;
                for (Connector connector : server.getConnectors()) {
                    System.out.println("==============================================");
                    countx++;
                    if (connector instanceof ServerConnector) {
                        ServerConnector serverConnector = (ServerConnector) connector;
//                        serverConnector.setIdleTimeout(300 * 1000);
                    }
                }
                
                System.out.println("countx: " + countx);
            }
        });
        return factory;
    }

}
