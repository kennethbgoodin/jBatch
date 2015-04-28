package de.slikey.batch.protocol.job;

import de.slikey.batch.network.protocol.Packet;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Kevin Carstens
 * @since 28.04.2015
 */
public class PacketJobConsoleOutput extends Packet {

    private UUID uuid;
    private Level level;
    private String line;

    public PacketJobConsoleOutput() {

    }

    public PacketJobConsoleOutput(UUID uuid, Level level, String line) {
        this.uuid = uuid;
        this.level = level;
        this.line = line;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    @Override
    public void write(ByteBuf buf) throws IOException {
        writeUUID(buf, uuid);
        writeLevel(buf, level);
        writeString(buf, line);
    }

    @Override
    public void read(ByteBuf buf) throws IOException {
        uuid = readUUID(buf);
        level = readLevel(buf);
        line = readString(buf);
    }

    private static void writeLevel(ByteBuf buf, Level code) {
        buf.writeByte(code.ordinal());
    }

    private static Level readLevel(ByteBuf buf) {
        return Level.values()[buf.readByte()];
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "uuid=" + uuid +
                ", level=" + level +
                ", line='" + line + '\'' +
                '}';
    }

    public static enum Level {

        OUTPUT, ERROR

    }
}
