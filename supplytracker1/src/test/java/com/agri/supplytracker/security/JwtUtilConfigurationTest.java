package com.agri.supplytracker.security;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class JwtUtilConfigurationTest {
    @Test void missingSecretFailsConfigurationValidation(){assertThrows(IllegalStateException.class,()->new JwtUtil().validateConfiguration());}
    @Test void weakSecretFailsConfigurationValidation(){JwtUtil jwt=new JwtUtil();ReflectionTestUtils.setField(jwt,"SECRET_KEY","too-short");assertThrows(IllegalStateException.class,jwt::validateConfiguration);}
}
