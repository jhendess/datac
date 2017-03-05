package org.xlrnet.datac.session.services;

import org.junit.Test;
import org.xlrnet.datac.session.domain.validation.PasswordValidator;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests for {@link PasswordValidator}.
 */
public class PasswordServiceTest {

    private PasswordService passwordService = new PasswordService();

    @Test
    public void testValidPasswords() {
        assertTrue(passwordService.isValid("aBc123"));
        assertTrue(passwordService.isValid("aBc123$"));
        assertTrue(passwordService.isValid("$abc123$"));
        assertTrue(passwordService.isValid(".AB123$"));
    }

    @Test
    public void testInvalidPasswords() {
        assertFalse(passwordService.isValid("abc123"));      // Missing group
        assertFalse(passwordService.isValid(" AcB123$"));    // Whitespace
    }

}