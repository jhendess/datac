package org.xlrnet.datac.session.services;

import org.junit.Test;
import org.xlrnet.datac.session.domain.validation.PasswordValidator;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests for {@link PasswordValidator}.
 */
public class CryptoServiceTest {

    private CryptoService cryptoService = new CryptoService();

    @Test
    public void testValidPasswords() {
        assertTrue(cryptoService.isValid("aBc123"));
        assertTrue(cryptoService.isValid("aBc123$"));
        assertTrue(cryptoService.isValid("$abc123$"));
        assertTrue(cryptoService.isValid(".AB123$"));
    }

    @Test
    public void testInvalidPasswords() {
        assertFalse(cryptoService.isValid("abc123"));      // Missing group
        assertFalse(cryptoService.isValid(" AcB123$"));    // Whitespace
    }

}