package com.verilogic.ui;

import com.verilogic.ui.views.CaseWorkbenchView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CaseWorkbenchViewTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void instantiateCaseWorkbenchView() {
        CaseWorkbenchView view = applicationContext.getAutowireCapableBeanFactory().createBean(CaseWorkbenchView.class);
        assertThat(view).isNotNull();
    }
}
