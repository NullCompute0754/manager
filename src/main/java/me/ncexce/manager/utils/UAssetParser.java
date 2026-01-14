package me.ncexce.manager.utils;

import me.ncexce.manager.pojo.ParsedHashEntry;
import me.ncexce.manager.pojo.ParsedHashing;
import me.ncexce.manager.pojo.ParsedUAsset;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UAssetParser {

    private UAssetParser() {}

    public static ParsedUAsset parse(File file) throws IOException, IllegalArgumentException {

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            ByteOrder endian = ByteOrder.LITTLE_ENDIAN;

            PackageMetadata metadata = readSummary(raf, endian);

            int tag = metadata.tag;
            int legacyVersion = metadata.legacyVersion;
            int legacyUE3Version = metadata.legacyUE3Version;
            int fileVersionUE4 = metadata.fileVersionUE4;
            int fileVersionUE5 = metadata.fileVersionUE5;
            int licenseeVersion = metadata.licenseeVersion;

            // Hashing
            String masterUUID = metadata.hashing.masterUUID;
            int auxByte1 = metadata.hashing.auxByte1;
            int auxByte2 = metadata.hashing.auxByte2;
            int entryCount = metadata.hashing.entryCount;

            List<ParsedHashEntry> parsedEntries= new ArrayList<>();
            if (metadata.hashing.entries != null) {
                for (HashingEntry he : metadata.hashing.entries) {
                    ParsedHashEntry entry = new ParsedHashEntry(he.entryUUID, he.entryChecksum);
                    parsedEntries.add(entry);
                }
            }

            ParsedHashing hashing = new ParsedHashing(masterUUID, auxByte1, auxByte2, entryCount, parsedEntries);

            String assetLocation = metadata.assetLocation;
            int nameCount = metadata.nameCount;
            int nameOffset = metadata.nameOffset;

            int unkCount = metadata.unkCount;
            int unkOffset = metadata.unkOffset;

            List<String> names= new ArrayList<>(metadata.names);

            return new ParsedUAsset(tag,
                    legacyVersion,
                    legacyUE3Version,
                    fileVersionUE4,
                    fileVersionUE5,
                    licenseeVersion,
                    hashing,
                    assetLocation,
                    nameCount,
                    nameOffset,
                    unkCount,
                    unkOffset,
                    names);
        }
    }

    private static PackageMetadata readSummary(RandomAccessFile raf, ByteOrder endian) throws IOException, IllegalArgumentException {
        PackageMetadata metadata = new PackageMetadata();
        metadata.hashing = new HashingStructure();

        metadata.tag = readInt(raf, endian);
        if (metadata.tag != 0x9E2A83C1) {
            throw new IllegalArgumentException("Invalid uasset file: magic tag mismatch.");
        }

        metadata.legacyVersion = readInt(raf, endian);
        if(metadata.legacyVersion != 0xFFFFFFF7) {
            throw new IllegalArgumentException("Invalid uasset file: legacy version is not supported.");
        }

        metadata.legacyUE3Version = readInt(raf, endian);
        metadata.fileVersionUE4 = readInt(raf, endian);
        metadata.fileVersionUE5 = readInt(raf, endian);
        metadata.licenseeVersion = readInt(raf, endian);

        metadata.hashing.masterUUID=readString(raf, 16); //not readFString(raf, endian); //reserved
        metadata.hashing.auxByte1 = readInt(raf, endian);
        metadata.hashing.auxByte2 = readInt(raf, endian);
        metadata.hashing.entryCount = readInt(raf, endian);
        if(metadata.hashing.entryCount >= 1) {
            metadata.hashing.entries = new HashingEntry[metadata.hashing.entryCount];
            for(int st=0; st<metadata.hashing.entryCount; st++) {
                metadata.hashing.entries[st] = new HashingEntry();
                metadata.hashing.entries[st].entryUUID = readString(raf, 16); //not readFString(raf, endian); //reserved
                metadata.hashing.entries[st].entryChecksum = readInt(raf, endian);
            }
        }

        metadata.assetLocation = readFString(raf, endian);
        raf.skipBytes(4); // String checksum
        metadata.nameCount = readInt(raf, endian);
        metadata.nameOffset = readInt(raf, endian);

        metadata.unkCount = readInt(raf, endian);
        metadata.unkOffset = readInt(raf, endian);

        metadata.names = readNameMap(raf, metadata.nameOffset, metadata.nameCount, endian);
        return metadata;
    }

    private static List<String> readNameMap(RandomAccessFile raf, int offset, int count, ByteOrder endian) throws IOException {
        List<String> names = new ArrayList<>();
        String name;
        raf.seek(offset);

        for (int i = 0; i < count; i++) {
            name = readFString(raf, endian);
            names.add(name);
            raf.skipBytes(4); // name flags
        }

        return names;
    }

    private static String readString(RandomAccessFile raf, int length) throws IOException {
        byte[] buf = new byte[length];
        raf.readFully(buf);
        String res = ByteUtils.bytesToHex(buf);
        return res;
    }

    // 更简洁的版本，使用CharsetDecoder
    private static String readFString(RandomAccessFile raf, ByteOrder endian) throws IOException {
        int len = readInt(raf, endian);
    
        if (len == 0) return "";
    
        if (len > 0) {
            // UTF-8
            byte[] buf = new byte[len];
            raf.readFully(buf);
            // 去掉null终止符
            return new String(buf, 0, len - 1, StandardCharsets.UTF_8);
        } else {
            // UTF-16
            int charCount = -len;
            int byteLen = charCount*2;
            byte[] bytes = new byte[byteLen];
            raf.readFully(bytes);
        
            // 去掉null终止符（最后2个字节）
            ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, byteLen - 2);
            buffer.order(endian);
        
            // 使用UTF-16解码器
            Charset charset = endian == ByteOrder.LITTLE_ENDIAN ? 
                StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
        
            return charset.decode(buffer).toString();
        }
    }


    private static int readInt(RandomAccessFile raf, ByteOrder endian) throws IOException {
        byte[] bytes = new byte[4];
        raf.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(endian).getInt();
    }

    private static class PackageMetadata {
        int tag;

        int legacyVersion;
        int legacyUE3Version;
        int fileVersionUE4;
        int fileVersionUE5;
        int licenseeVersion;

        HashingStructure hashing;

        String assetLocation;

        int nameCount;
        int nameOffset;

        int unkCount;
        int unkOffset;

        List<String> names;

    }

    private static class HashingStructure {
        String masterUUID;
        int auxByte1;
        int auxByte2;
        int entryCount;
        HashingEntry[] entries;
    }

    private static class HashingEntry {
        String entryUUID;
        int entryChecksum;
    }

    public static void movePointer(RandomAccessFile raf, long offset) throws IOException {
        long newPos = raf.getFilePointer() + offset;
        if (newPos < 0) newPos = 0;
        raf.seek(newPos);
    }
}
