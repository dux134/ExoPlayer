package com.google.android.exoplayer2.demo.offline;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by sharish on 29/01/18.
 */

public class FileUtils {

    private static final String CHARSET = "UTF-8";

    /**
     * Initial Vector Length
     */
    private static final int IV_LEN = 16;

    public static void writeJson(File folder, String id, JSONObject object) {

        FileOutputStream fos = null;

        if (!folder.exists()) {
            folder.mkdir();
        }
        try {

            byte[] bytes = object.toString().getBytes("UTF-8");
            fos = new FileOutputStream(new File(folder, id));
            fos.write(bytes, 0, bytes.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    /**
     * Writes the given JSON into a file after encrypting it with the given encryptionKey.
     *
     * @param targetDir     - Directory where the file has to be created.
     * @param name          - File name
     * @param encryptionKey - 16 byte encryption key for encryption
     * @param object        - JSON data.
     */
    public static void writeEncryptedJson(File targetDir, String name, @NonNull byte[] encryptionKey, @NonNull JSONObject object) {

        DataOutputStream output = null;

        try {

            if (!targetDir.exists()) {
                targetDir.mkdir();
            }

            byte[] contentBytes = object.toString().getBytes(CHARSET);

            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(new File(targetDir, name)));
            output = new DataOutputStream(outStream);

            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, "AES");
            Cipher cipher = getCipher();

            byte[] initializationVector = new byte[IV_LEN];
            new Random().nextBytes(initializationVector);
            output.write(initializationVector);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new IllegalStateException(e); // Should never happen.
            }
            output.flush();
            output = new DataOutputStream(new CipherOutputStream(outStream, cipher));

            output.writeInt(contentBytes.length);
            output.write(contentBytes);
            output.flush();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads the file content as JSON object after performing decryption with the given decryption key.
     *
     * @param targetDir     - Directory where the file has to be created.
     * @param name          - File name
     * @param decryptionKey - 16 byte decryption key.
     * @return Ouput JSON when successful, null when error occurred.
     */
    public static JSONObject readEncryptedJson(File targetDir, String name, byte[] decryptionKey) {

        DataInputStream input = null;
        File file = new File(targetDir, name);

        if (!file.exists()) return null;
        try {

            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            input = new DataInputStream(inputStream);

            SecretKeySpec secretKeySpec = new SecretKeySpec(decryptionKey, "AES");
            Cipher cipher = getCipher();

            byte[] initializationVector = new byte[IV_LEN];
            input.readFully(initializationVector);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
            try {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new IllegalStateException(e);
            }
            input = new DataInputStream(new CipherInputStream(inputStream, cipher));


            int totalLen = input.readInt();
            byte[] contentBytes = new byte[totalLen];
            input.readFully(contentBytes);

            String readString = new String(contentBytes);

            return new JSONObject(readString);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null)
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;

    }

    public static JSONObject readJson(File mainFolder, String id) {

        FileInputStream fis = null;
        if (!mainFolder.exists()) {
            mainFolder.mkdir();
        }

        File file = new File(mainFolder, id);

        if (!file.exists()) return null;
        try {
            fis = new FileInputStream(file);
            int length = (int) file.length();
            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);
            fis.close();

            return new JSONObject(new String(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;

    }

    public static void writeBytes(File mainFolder, String id, byte[] keyId) {

        FileOutputStream fos = null;

        if (!mainFolder.exists()) {
            mainFolder.mkdir();
        }
        try {
            fos = new FileOutputStream(new File(mainFolder, id));
            fos.write(keyId, 0, keyId.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public static byte[] readBytes(File mainFolder, String id) {

        FileInputStream fis = null;
        if (!mainFolder.exists()) {
            mainFolder.mkdir();
        }

        File file = new File(mainFolder, id);

        if (!file.exists()) return null;
        try {
            fis = new FileInputStream(file);
            int length = (int) file.length();
            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);
            fis.close();

            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;
    }

    /**
     * Copied from com.google.android.exoplayer2.upstream.cache.CachedContentIndex#getCipher()
     */
    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        // Workaround for https://issuetracker.google.com/issues/36976726
        if (Util.SDK_INT == 18) {
            try {
                return Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
            } catch (Throwable ignored) {
                // ignored
            }
        }
        return Cipher.getInstance("AES/CBC/PKCS5PADDING");
    }
}
