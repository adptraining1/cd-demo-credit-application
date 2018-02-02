package com.innoq.mploed.ddd.application.integration;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.innoq.mploed.ddd.application.domain.CreditApplicationForm;
import com.innoq.mploed.ddd.application.domain.Customer;
import com.innoq.mploed.ddd.scoring.shared.ScoringInput;
import com.innoq.mploed.ddd.scoring.shared.ScoringResult;
import com.innoq.mploed.ddd.scoring.shared.ScoringService;
import org.springframework.stereotype.Service;

@Service
public class ScoringClient {
    private ScoringService scoringService;

    private Timer scoringTimer;

    public ScoringClient(ScoringService scoringService, MetricRegistry metricRegistry) {
        this.scoringService = scoringService;
        this.scoringTimer = metricRegistry.timer("scoring-times");

    }

    public ScoringResult performScoring(CreditApplicationForm creditApplicationForm, Customer customer) {
        ScoringInput scoringInput = new ScoringInput();
        scoringInput.setIncome(creditApplicationForm.getSelfDisclosure().getEarnings().sum());
        scoringInput.setSpendings(creditApplicationForm.getSelfDisclosure().getOutgoings().sum());
        scoringInput.setReason(creditApplicationForm.getPurpose());
        scoringInput.setMonthlyPayment(creditApplicationForm.getMonthlyPayment().longValue());
        scoringInput.setFirstName(customer.getFirstName());
        scoringInput.setLastName(customer.getLastName());
        scoringInput.setStreet(customer.getStreet());
        scoringInput.setPostCode(customer.getPostCode());
        ScoringResult scoringResult = null;
        Timer.Context timer = scoringTimer.time();
        try {
            scoringResult = scoringService.performScoring(scoringInput);
        } finally {
            timer.stop();
        }


        return scoringResult;
    }

}
