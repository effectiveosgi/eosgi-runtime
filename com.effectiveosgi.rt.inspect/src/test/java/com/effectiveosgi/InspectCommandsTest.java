package com.effectiveosgi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.osgi.service.cm.Configuration;

public class InspectCommandsTest {

    @Test
    public void testRepeats() {
        assertEquals("----------", ComponentCommands.repeat(10, '-'));
        assertEquals("---", ComponentCommands.repeatForDigits(999, '-'));
        assertEquals("----", ComponentCommands.repeatForDigits(1000, '-'));
    }
    


}
