package com.yukms.yamlxbeans;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class MergeTest {

    @Test
    public void testMerge() throws FileNotFoundException, YamlxException {
        InputStream input = new FileInputStream("test/test-merge.yml");
        Reader reader = new InputStreamReader(input);
        Map data = new YamlxReader(reader).read(Map.class);
        Map stuff = (Map) data.get("merged");
        assertEquals("v1", stuff.get("v1"));
        assertEquals("v2", stuff.get("v2"));
        assertEquals("v3", stuff.get("v3"));
        assertNull(stuff.get("<<"));
    }

}
