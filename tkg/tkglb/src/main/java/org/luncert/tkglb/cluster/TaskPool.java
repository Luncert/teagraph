package org.luncert.tkglb.cluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TaskPool {

    private static class Group {

        private List<Task> tasks = new ArrayList<>();
        private List<Result> results = new ArrayList<>();
        private Consumer<String> callback;

        Group(Consumer<String> callback) {
            this.callback = callback;
        }

        void addWaitingTask(Task task) {
            tasks.add(task);
        }

        void addTaskResult(Result result) {
            results.add(result);
        }

        boolean executeFinished() {
            return tasks.size() == results.size();
        }

        /**
         * 合并执行结果,还没实现
         * @return
         */
        String reduce() {
            // 根据taskId对执行结果排序
            results.sort(new Comparator<Result>() {
                public int compare(Result r1, Result r2) {
                    return r1.getTaskId() > r2.getTaskId() ? 1 : 
                            (r1.getTaskId() < r2.getTaskId() ? -1 : 0);
                }
            });
            StringBuilder builder = new StringBuilder();
            for (int i = 0, limit = results.size(); i < limit; i++) {
                builder.append(results.get(i));
            }
            return builder.toString();
        }

    }

    private Map<Integer, Group> groups = new ConcurrentHashMap<>();

    /**
     * 创建新的任务组,当任务组所有的任务都执行结束后调用callback,并自动删除任务组
     * @param gid
     * @param callback
     */
    public void newGroup(int gid, Consumer<String> callback) {
        Group g = new Group(callback);
        groups.put(gid, g);
    }

    private Group getGroup(int gid) {
        Group g = groups.get(gid);
        if (g == null)
            throw new NoSuchElementException("Invalid group ID: " + gid);
        return g;
    }

    /**
     * 添加任务到所属任务组
     * @param task
     */
    public void addWaitingTask(Task task) {
        int gid = task.getGroupId();
        getGroup(gid).addWaitingTask(task);
    }

    /**
     * 添加执行结果到任务组
     * @param result
     */
    public void addTaskResult(Result result) {
        int gid = result.getGroupId();
        Group g = getGroup(gid);
        g.addTaskResult(result);
        if (g.executeFinished()) {
            g.callback.accept(g.reduce());
            groups.remove(gid);
        }
    }

}