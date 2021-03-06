/*
 *    Copyright 2016 Conor Nosal
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.cjnosal.secret_storage;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import com.github.cjnosal.secret_storage.keymanager.KeyManager;
import com.github.cjnosal.secret_storage.keymanager.defaults.DefaultManagers;
import com.github.cjnosal.secret_storage.storage.DataStorage;
import com.github.cjnosal.secret_storage.storage.defaults.DefaultStorage;
import com.github.cjnosal.secret_storage.storage.encoding.DataEncoding;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecretStorage {

    private Context context;
    private String storeId;
    private DataStorage configStorage;
    private DataStorage dataStorage;
    private KeyManager keyManager;

    // TODO test multiple instances of SecretStorage with same/different key managers to ensure no conflicts if different stores use same data ID

    public SecretStorage(Context context, String storeId, DataStorage configStorage, DataStorage dataStorage, KeyManager keyManager) throws IOException, GeneralSecurityException {
        this.context = context;
        this.storeId = storeId;
        this.configStorage = configStorage;
        this.dataStorage = dataStorage;
        this.keyManager = keyManager;
    }

    public SecretStorage(Context context, String storeId, @Nullable String userPassword) throws IOException, GeneralSecurityException {
        this.context = context;
        this.storeId = storeId;
        this.configStorage = createStorage(DataStorage.TYPE_CONF);
        this.dataStorage = createStorage(DataStorage.TYPE_DATA);
        this.keyManager = selectKeyManager(userPassword);
    }

    public void store(String id, byte[] plainText) throws GeneralSecurityException, IOException {
        byte[] cipherText = keyManager.encrypt(id, plainText);
        dataStorage.store(id, cipherText);
    }

    public byte[] load(String id) throws GeneralSecurityException, IOException {
        byte[] cipherText = dataStorage.load(id);
        return keyManager.decrypt(id, cipherText);
    }

    private KeyManager selectKeyManager(@Nullable String userPassword) throws IOException, GeneralSecurityException {
        int osVersion; // OS Version when store was created // TODO migrations
        if (configStorage.exists(storeId + "Version")) {
            osVersion = DataEncoding.decodeInt(configStorage.load(storeId + "Version"));
        } else {
            osVersion = Build.VERSION.SDK_INT;
            configStorage.store(storeId + "Version", DataEncoding.encode(osVersion));
        }

        return new DefaultManagers().selectDefaultManager(context, osVersion, configStorage, createStorage(DataStorage.TYPE_KEYS), storeId, userPassword);
    }

    private DataStorage createStorage(@DataStorage.Type String type) {
        return new DefaultStorage().createStorage(context, storeId, type);
    }
}
