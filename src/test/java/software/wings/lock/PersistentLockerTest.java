package software.wings.lock;

import org.junit.Test;
import software.wings.WingsBaseUnitTest;

import javax.inject.Inject;

public class PersistentLockerTest extends WingsBaseUnitTest {
  @Inject private PersistentLocker persistentLocker;

  @Test
  public void testAcquireLock() {
    String uuid = "" + System.currentTimeMillis();
    System.out.println("uuid : " + uuid);
    boolean acquired = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired : " + acquired);

    boolean acquired2 = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired2 : " + acquired2);

    //		boolean released = persistentLocker.releaseLock("abc", uuid);
    //		System.out.println("released : " + released);

    //		boolean acquired3 = persistentLocker.acquireLock("abc", uuid);
    //		System.out.println("acquired3 : " + acquired3);
  }
}
