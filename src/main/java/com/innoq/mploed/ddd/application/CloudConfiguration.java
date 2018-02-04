package com.innoq.mploed.ddd.application;

import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;

@Configuration
@Profile("cloud")
public class CloudConfiguration extends AbstractCloudConfig {

	@Bean
	public DataSource relationalDataSource() {
		PooledServiceConnectorConfig.PoolConfig poolConfig = new PooledServiceConnectorConfig.PoolConfig(5, 30, 3000);
		DataSourceConfig dataSourceConfig = new DataSourceConfig(poolConfig, null);
		return connectionFactory().dataSource("mysql-credit-application-db", dataSourceConfig);
	}

	@Bean
	public RedisConnectionFactory redisFactory() {
		return connectionFactory().redisConnectionFactory();
	}
}