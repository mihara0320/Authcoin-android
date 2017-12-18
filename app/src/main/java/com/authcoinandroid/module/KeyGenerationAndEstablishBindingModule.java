package com.authcoinandroid.module;

import android.util.Pair;

import com.authcoinandroid.model.EirIdentifier;
import com.authcoinandroid.model.EntityIdentityRecord;
import com.authcoinandroid.service.identity.EirRepository;
import com.authcoinandroid.service.keypair.KeyPairService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

/**
 * "KeyGenerationEstablishBinding" module
 * <p>
 * Differences: see {@link KeyGenerationModule} and {@link EstablishBindingModule} submodules.
 */
public class KeyGenerationAndEstablishBindingModule {

    private final EstablishBindingModule bindingModule = new EstablishBindingModule();
    private final KeyGenerationModule keyGenerator;
    private final EirRepository repository;

    public KeyGenerationAndEstablishBindingModule(EirRepository repository, KeyPairService keyPairService) {
        this.repository = repository;
        this.keyGenerator = new KeyGenerationModule(keyPairService);
    }

    public Pair<KeyPair, EntityIdentityRecord> generateAndEstablishBinding(String[] identifiers, String alias)
            throws GeneralSecurityException, IOException {
        KeyPair keyPair = keyGenerator.createNewKeyPair(alias);
        EntityIdentityRecord eir = bindingModule.establishBinding(identifiers, keyPair, alias);
        return Pair.create(keyPair, eir);
    }

    /**
     * "CreateNewKeyPair" submodule.
     * <p>
     * Differences:
     * 1. Id is replaced by alias. Alias is used to store the key to Android KeyStore.
     * 2. Android KeyPair generator is used instead of OpenSSH.
     * 3. EC key is generated. No input parameters defined in master thesis is needed.
     */
    public class KeyGenerationModule {

        private final KeyPairService keyPairService;

        public KeyGenerationModule(KeyPairService keyPairService) {
            this.keyPairService = keyPairService;
        }

        public KeyPair createNewKeyPair(String alias) throws GeneralSecurityException {
            return keyPairService.create(alias);
        }

    }

    /**
     * "EstablishBinding" submodule.
     * <p>
     * Differences:
     * 1. ID parameter is generated by smart contract (hash(pubKey))
     * 2. Timestamp is replaced by block number and is added by smart contract.
     */
    public class EstablishBindingModule {

        public EntityIdentityRecord establishBinding(String[] identifiers, KeyPair keyPair, String alias) throws GeneralSecurityException, IOException {
            EntityIdentityRecord eir = new EntityIdentityRecord(
                    alias,
                    keyPair
            );
            for (String identifier : identifiers) {
                EirIdentifier eirIdentifier = new EirIdentifier(identifier);
                eirIdentifier.setEir(eir);
                eir.getIdentifiers().add(eirIdentifier);
            }

            eir = repository.save(eir).blockingGet();
            return eir;
        }
    }
}
