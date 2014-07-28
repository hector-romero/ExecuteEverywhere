package me.vemacs.executeeverywhere;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.io.*;

public class ExecuteEverywhere extends Plugin implements Listener {
    private JedisPool pool;
    private final String BUNGEE_CHANNEL = "eb";
    private static Plugin instance;

    Configuration config;

    @Override
    public void onEnable() {
        instance = this;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(
                    loadResource(this, "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ip = config.getString("ip");
        int port = config.getInt("port");
        String password = config.getString("password");
        if (password == null || password.equals(""))
            pool = new JedisPool(new JedisPoolConfig(), ip, port, 0);
        else
            pool = new JedisPool(new JedisPoolConfig(), ip, port, 0, password);
        Jedis jedis = pool.getResource();
        try {
            jedis.subscribe(new EESubscriber(), BUNGEE_CHANNEL);
        } catch (Exception e) {
            e.printStackTrace();
            pool.returnBrokenResource(jedis);
            getLogger().severe("Unable to connect to Redis server.");
            return;
        }
        pool.returnResource(jedis);
    }

    public class EESubscriber extends JedisPubSub {
        @Override
        public void onMessage(String channel, final String msg) {
            ExecuteEverywhere.instance.getLogger().info("Dispatching /" + msg);
            ProxyServer ps = ProxyServer.getInstance();
            ps.getPluginManager().dispatchCommand(ps.getConsole(), msg);
        }

        @Override
        public void onPMessage(String s, String s2, String s3) {
        }

        @Override
        public void onSubscribe(String s, int i) {
        }

        @Override
        public void onUnsubscribe(String s, int i) {
        }

        @Override
        public void onPUnsubscribe(String s, int i) {
        }

        @Override
        public void onPSubscribe(String s, int i) {
        }
    }

    public static File loadResource(Plugin plugin, String resource) {
        File folder = plugin.getDataFolder();
        if (!folder.exists())
            folder.mkdir();
        File resourceFile = new File(folder, resource);
        try {
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                try (InputStream in = plugin.getResourceAsStream(resource);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }
}

