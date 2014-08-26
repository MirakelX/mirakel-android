package de.azapps.mirakel.model.account;

import de.azapps.mirakel.model.ModelVanishedException;

public class AccountVanishedException extends ModelVanishedException {

    public AccountVanishedException() {
        super();
    }

    public AccountVanishedException(String message) {
        super(message);
    }

    public AccountVanishedException(long id) {
        super(id);
    }
}
