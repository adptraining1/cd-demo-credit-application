package com.innoq.mploed.ddd.application.controller;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.innoq.mploed.ddd.application.domain.CreditApplicationForm;
import com.innoq.mploed.ddd.application.domain.Customer;
import com.innoq.mploed.ddd.application.events.CreditApplicationApprovedEvent;
import com.innoq.mploed.ddd.application.events.CreditApplicationDeclinedEvent;
import com.innoq.mploed.ddd.application.events.EventPublisher;
import com.innoq.mploed.ddd.application.integration.CustomerClient;
import com.innoq.mploed.ddd.application.integration.ScoringClient;
import com.innoq.mploed.ddd.application.repository.CreditApplicationFormRespository;
import com.innoq.mploed.ddd.scoring.shared.ScoringColor;
import com.innoq.mploed.ddd.scoring.shared.ScoringResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
@RequestMapping(path = "/")
public class CreditApplicationController {

    private CreditApplicationFormRespository creditApplicationFormRespository;

    private CustomerClient customerClient;

    private ScoringClient scoringClient;

    private EventPublisher eventPublisher;

    private Histogram earningsHistogram;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditApplicationController.class);

    @Autowired
    public CreditApplicationController(CreditApplicationFormRespository creditApplicationFormRespository,
                                       CustomerClient customerClient,
                                       ScoringClient scoringClient,
                                       EventPublisher eventPublisher,
                                       MetricRegistry metricRegistry) {
        this.creditApplicationFormRespository = creditApplicationFormRespository;
        this.customerClient = customerClient;
        this.scoringClient = scoringClient;
        this.eventPublisher = eventPublisher;
        this.earningsHistogram = metricRegistry.histogram("earnings");
    }

    @RequestMapping(method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("processContainer", new ProcessContainer());
        return "index";
    }

    @RequestMapping(method = RequestMethod.POST, path = "saveStepOne")
    public String saveStepOne(@ModelAttribute ProcessContainer processContainer, Model model) {
        CreditApplicationForm savedCreditApplication = creditApplicationFormRespository.saveAndFlush(processContainer.getCreditApplicationForm());
        processContainer.setCreditApplicationForm(savedCreditApplication);
        earningsHistogram.update(processContainer.getCreditApplicationForm().getSelfDisclosure().getEarnings().sum());
        model.addAttribute("processContainer", processContainer);
        return "stepTwo";
    }

    @RequestMapping(method = RequestMethod.POST, path = "saveStepTwo")
    public String saveStepTwo(@ModelAttribute ProcessContainer processContainer, Model model) {

        LOGGER.info("Remotely and synchronously calling the Customer Application in order to save the customer");
        Customer customer = customerClient.saveCustomer(processContainer.getCustomer());
        CreditApplicationForm creditApplicationForm = creditApplicationFormRespository.findOne(processContainer.getCreditApplicationForm().getId());

        processContainer.setCustomer(customer);
        processContainer.setCreditApplicationForm(creditApplicationForm);

        creditApplicationForm.setCustomerId(customer.getId());
        creditApplicationFormRespository.save(creditApplicationForm);
        model.addAttribute("processContainer", processContainer);

        return "applicationSummary";
    }

    @RequestMapping(method = RequestMethod.POST, path = "performScoring")
    public String performScoring(@ModelAttribute ProcessContainer processContainer, Model model) {

        CreditApplicationForm creditApplicationForm = creditApplicationFormRespository.findOne(processContainer.getCreditApplicationForm().getId());

        LOGGER.info("Remotely and synchronously calling the Scoring Application in order to perform a scoring");
        ScoringResult scoringResult = scoringClient.performScoring(creditApplicationForm, processContainer.getCustomer());

        if(scoringResult.getScoringColor().equals(ScoringColor.GREEN)) {
            LOGGER.info("Scoring was green, sending CreditApplicationApprovedEvent");
            eventPublisher.publishEvent("credit-application-approved-events", new CreditApplicationApprovedEvent(creditApplicationForm));
        } else {
            LOGGER.info("Scoring was NOT green, sending CreditApplicationDeclinedEvent");
            eventPublisher.publishEvent("credit-application-declined-events", new CreditApplicationDeclinedEvent(creditApplicationForm));
        }

        model.addAttribute("scoringResult", scoringResult);

        return "scoringResult";
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public void deleteEverything() {
        creditApplicationFormRespository.deleteAll();
    }
}
