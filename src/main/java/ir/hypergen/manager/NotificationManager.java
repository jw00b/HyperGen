package ir.hypergen.manager;

import ir.hypergen.HyperGen;
import ir.hypergen.model.GenerationTask;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NotificationManager {
    private final HyperGen plugin;
    private String discordWebhook;
    private String customWebhook;
    private boolean enableDiscord;
    private boolean enableCustom;
    
    public NotificationManager(HyperGen plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        this.discordWebhook = plugin.getConfig().getString("notifications.discord.webhook", "");
        this.customWebhook = plugin.getConfig().getString("notifications.custom.webhook", "");
        this.enableDiscord = plugin.getConfig().getBoolean("notifications.discord.enabled", false);
        this.enableCustom = plugin.getConfig().getBoolean("notifications.custom.enabled", false);
    }
    
    public void notifyTaskStart(GenerationTask task) {
        String message = String.format("✅ **تولید چانک شروع شد**\n" +
            "🌍 دنیا: %s\n" +
            "⚙️ حالت: %s\n" +
            "📊 تعداد چانک‌ها: %d",
            task.getWorld().getName(),
            task.getMode().name(),
            task.getTotalChunks()
        );
        
        sendNotification(message, 0x00FF00);
    }
    
    public void notifyTaskComplete(GenerationTask task) {
        long elapsed = task.getElapsedTime();
        double speed = (double) task.getCurrentChunk() / (elapsed / 1000.0);
        
        String message = String.format("🎉 **تولید چانک تکمیل شد**\n" +
            "🌍 دنیا: %s\n" +
            "📊 چانک‌های تولید شده: %d\n" +
            "⏱️ زمان: %s\n" +
            "⚡ سرعت میانگین: %.2f chunks/s",
            task.getWorld().getName(),
            task.getCurrentChunk(),
            formatTime(elapsed),
            speed
        );
        
        sendNotification(message, 0x00FF00);
    }
    
    public void notifyTaskCancel(GenerationTask task) {
        String message = String.format("⚠️ **تولید چانک لغو شد**\n" +
            "🌍 دنیا: %s\n" +
            "📊 پیشرفت: %.2f%%\n" +
            "📦 چانک‌های تولید شده: %d/%d",
            task.getWorld().getName(),
            task.getProgress(),
            task.getCurrentChunk(),
            task.getTotalChunks()
        );
        
        sendNotification(message, 0xFFA500);
    }
    
    public void notifyProgress(GenerationTask task) {
        if (task.getCurrentChunk() % 1000 != 0) return;
        
        String message = String.format("📈 **پیشرفت تولید**\n" +
            "🌍 دنیا: %s\n" +
            "📊 پیشرفت: %.2f%%\n" +
            "📦 چانک‌ها: %d/%d\n" +
            "⏱️ زمان تخمینی: %s",
            task.getWorld().getName(),
            task.getProgress(),
            task.getCurrentChunk(),
            task.getTotalChunks(),
            estimateRemainingTime(task)
        );
        
        sendNotification(message, 0x0099FF);
    }
    
    public void notifyError(String error) {
        String message = String.format("❌ **خطا**\n%s", error);
        sendNotification(message, 0xFF0000);
    }
    
    private void sendNotification(String message, int color) {
        if (enableDiscord && !discordWebhook.isEmpty()) {
            sendDiscordWebhook(message, color);
        }
        
        if (enableCustom && !customWebhook.isEmpty()) {
            sendCustomWebhook(message);
        }
    }
    
    private void sendDiscordWebhook(String message, int color) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(discordWebhook);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String json = String.format(
                    "{\"embeds\":[{\"description\":\"%s\",\"color\":%d,\"footer\":{\"text\":\"HyperGen\"}}]}",
                    message.replace("\n", "\\n").replace("\"", "\\\""),
                    color
                );
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                
                conn.getResponseCode();
                conn.disconnect();
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord notification: " + e.getMessage());
            }
        });
    }
    
    private void sendCustomWebhook(String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(customWebhook);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String json = String.format("{\"message\":\"%s\"}", 
                    message.replace("\n", "\\n").replace("\"", "\\\""));
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                
                conn.getResponseCode();
                conn.disconnect();
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send custom webhook: " + e.getMessage());
            }
        });
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String estimateRemainingTime(GenerationTask task) {
        if (task.getCurrentChunk() == 0) return "محاسبه...";
        
        long elapsed = task.getElapsedTime();
        double progress = task.getProgress() / 100.0;
        
        if (progress == 0) return "محاسبه...";
        
        long totalEstimated = (long) (elapsed / progress);
        long remaining = totalEstimated - elapsed;
        
        return formatTime(remaining);
    }
    
    public void reload() {
        loadConfig();
    }
}