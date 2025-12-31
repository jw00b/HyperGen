package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import ir.hypergen.model.QueuedTask;
import ir.hypergen.model.Selection;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueManager {
    private final HyperGen plugin;
    private final Queue<QueuedTask> taskQueue;
    @Getter
    private QueuedTask currentTask;
    private boolean processing;
    private BukkitTask monitorTask;
    
    public QueueManager(HyperGen plugin) {
        this.plugin = plugin;
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.processing = false;
    }
    
    public void addToQueue(World world, Selection selection, GenerationTask.GenerationMode mode, int priority) {
        QueuedTask task = new QueuedTask(world, selection, mode, priority);
        taskQueue.add(task);
        
        sortQueue();
        
        if (!processing) {
            processNext();
        }
    }
    
    private void sortQueue() {
        List<QueuedTask> list = new ArrayList<>(taskQueue);
        list.sort(Comparator.comparingInt(QueuedTask::getPriority).reversed());
        taskQueue.clear();
        taskQueue.addAll(list);
    }
    
    public void processNext() {
        if (processing || taskQueue.isEmpty()) {
            return;
        }
        
        currentTask = taskQueue.poll();
        if (currentTask == null) {
            return;
        }
        
        processing = true;
        
        plugin.getTaskManager().startTask(
            currentTask.getWorld(),
            currentTask.getSelection(),
            currentTask.getMode()
        );
        
        startMonitoring();
    }
    
    private void startMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        
        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentTask == null) {
                return;
            }
            
            GenerationTask task = plugin.getTaskManager().getTask(currentTask.getWorld());
            
            if (task == null || !plugin.getTaskManager().hasActiveTask(currentTask.getWorld())) {
                processing = false;
                currentTask = null;
                processNext();
            }
        }, 20L, 20L);
    }
    
    public void cancelQueue() {
        taskQueue.clear();
        if (currentTask != null) {
            plugin.getTaskManager().cancelTask(currentTask.getWorld());
            currentTask = null;
        }
        processing = false;
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }
    
    public void removeFromQueue(int index) {
        List<QueuedTask> list = new ArrayList<>(taskQueue);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            taskQueue.clear();
            taskQueue.addAll(list);
        }
    }
    
    public List<QueuedTask> getQueue() {
        return new ArrayList<>(taskQueue);
    }
    
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    public boolean isProcessing() {
        return processing;
    }
    
    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        taskQueue.clear();
        currentTask = null;
        processing = false;
    }
}