package com.mi.replacemethod.gradle


import org.gradle.api.Action

/**
 * 为TraceMan自定义的配置项extension
 */
class Config {
    boolean open
    boolean openLog
    private final ReplaceByMethodCollector replaceByMethods
    /**
     * 只针对哪些 filter 显示日志
     */
    private final logFilters = new ArrayList(1)


    //创建内部Extension，名称为方法名 inner
    void replaceByMethods(Action<ReplaceByMethodCollector> action) {
        action.execute(replaceByMethods)
    }

    ReplaceByMethodCollector getReplaceByMethods() {
        return replaceByMethods
    }

    boolean getOpen() {
        return open
    }

    void open(boolean open) {
        this.open = open
    }

    void logFilters(String... filters) {
        if (filters != null && filters.length > 0) {
            logFilters.addAll(filters)
        }
    }

    List<String> getLogFilters() {
        return logFilters
    }

    boolean getOpenLog() {
        return openLog
    }

    void openLog(boolean openLog) {
        this.openLog = openLog
    }

    Config() {
        open = true
        openLog = true
        replaceByMethods = new ReplaceByMethodCollector()
    }


    @Override
    String toString() {
        return "Config{" +
                "open=" + open +
                ", openLog=" + openLog +
                ", replaceByMethods=" + replaceByMethods +
                ", logFilters=" + logFilters +
                '}';
    }
}

