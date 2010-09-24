package net.beaconcontroller.storage.memory.tests;

import net.beaconcontroller.storage.memory.MemoryStorageSource;
import net.beaconcontroller.storage.tests.StorageTest;
import org.junit.Before;

public class MemoryStorageTest extends StorageTest {

    @Before
    public void setUp() throws Exception {
        storageSource = new MemoryStorageSource();
        super.setUp();
    }
}
