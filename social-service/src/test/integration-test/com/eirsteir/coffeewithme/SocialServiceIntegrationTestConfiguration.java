package com.eirsteir.coffeewithme;


import com.eirsteir.coffeewithme.social.config.SocialServiceConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({SocialServiceConfiguration.class})
//@ComponentScan(basePackages = { "com.eirsteir.coffeewithme.social" }, excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { SetupTestDataLoader.class }) })
public class SocialServiceIntegrationTestConfiguration {
}