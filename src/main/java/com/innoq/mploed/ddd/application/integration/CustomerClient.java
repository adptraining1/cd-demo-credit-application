package com.innoq.mploed.ddd.application.integration;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.innoq.mploed.ddd.application.domain.Customer;
import com.innoq.mploed.ddd.application.integration.customer.wsdl.SaveCustomerRequest;
import com.innoq.mploed.ddd.application.integration.customer.wsdl.SaveCustomerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public class CustomerClient extends WebServiceGatewaySupport {
	private static final Logger log = LoggerFactory.getLogger(CustomerClient.class);

	private String customerServer;

	private Timer customerTimer;
	private Meter customerMeter;

	public CustomerClient(String customerServer, MetricRegistry metricRegistry) {
		this.customerServer = customerServer;
		this.customerMeter = metricRegistry.meter("customer-calls");
		this.customerTimer = metricRegistry.timer("customer-timer");
	}

	public Customer saveCustomer(Customer customer) {
		Timer.Context timer = customerTimer.time();
		Customer result = null;
		try {
			SaveCustomerRequest request = new SaveCustomerRequest();
			com.innoq.mploed.ddd.application.integration.customer.wsdl.Customer webServiceCustomer = new com.innoq.mploed.ddd.application.integration.customer.wsdl.Customer();
			webServiceCustomer.setCity(customer.getCity());
			webServiceCustomer.setFirstName(customer.getFirstName());
			webServiceCustomer.setLastName(customer.getLastName());
			webServiceCustomer.setStreet(customer.getStreet());
			webServiceCustomer.setPostCode(customer.getPostCode());
			request.setCustomer(webServiceCustomer);

			log.info("Saving Customer in the CRM System");

			SaveCustomerResponse response = (SaveCustomerResponse) getWebServiceTemplate().marshalSendAndReceive(customerServer + "ws", request);

			log.info("Saved Customer with Id: " + response.getCustomer().getId());
			result = new Customer();
			result.setId(response.getCustomer().getId());
			result.setFirstName(response.getCustomer().getFirstName());
			result.setLastName(response.getCustomer().getLastName());
			result.setStreet(response.getCustomer().getStreet());
			result.setPostCode(response.getCustomer().getPostCode());
			result.setCity(response.getCustomer().getCity());
			this.customerMeter.mark();
		} finally {
			timer.stop();
		}
		return result;
	}

}
