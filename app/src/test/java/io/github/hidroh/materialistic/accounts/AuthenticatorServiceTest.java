package io.github.hidroh.materialistic.accounts;

import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;

import io.github.hidroh.materialistic.test.TestRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(TestRunner.class)
public class AuthenticatorServiceTest {
    private AuthenticatorService service;
    private ServiceController<AuthenticatorService> controller;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(AuthenticatorService.class);
        service = controller.create().get();
    }

    @Test
    public void testBinder() {
        assertNotNull(service.onBind(new Intent()));
    }

    @After
    public void tearDown() {
        controller.destroy();
    }
}
