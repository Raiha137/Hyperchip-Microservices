package com.hyperchip.user.config;

import com.hyperchip.common.email.EmailService;
import org.springframework.stereotype.Service;

@Service
public class DummyEmailService implements EmailService {

    @Override
    public boolean sendEmail(String to, String subject, String body) {
        // do nothing, app should not fail
        return true;
    }
}
