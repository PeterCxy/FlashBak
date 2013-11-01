package us.shandian.flashbak.helper;

import java.util.Comparator;
import java.util.Map;

public class BackupItemComparator implements Comparator
{
    public int compare(Object o1, Object o2) {
		Map<String, Object> map1 = (Map<String, Object>) o1;
		Map<String, Object> map2 = (Map<String, Object>) o2;
		if (Long.valueOf(map1.get("timestamp").toString()) > Long.valueOf(map2.get("timestamp").toString())) {
			return -1;
		} else {
			return 1;
		}
	}
}
