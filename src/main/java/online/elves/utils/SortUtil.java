package online.elves.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排序工具类
 */
public class SortUtil {
    /**
     * Map按照整数型的value进行降序排序，当value相同时，按照key的长度进行排序
     *
     * @param map
     * @return
     */
    public static LinkedHashMap<String, Integer> sortMapWithValue(Map<String, Integer> map) {
        return map.entrySet().stream().sorted(((item1, item2) -> {
            int compare = item2.getValue().compareTo(item1.getValue());
            if (compare == 0) {
                if (item1.getKey().length() < item2.getKey().length()) {
                    compare = 1;
                } else if (item1.getKey().length() > item2.getKey().length()) {
                    compare = -1;
                }
            }
            return compare;
        })).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
