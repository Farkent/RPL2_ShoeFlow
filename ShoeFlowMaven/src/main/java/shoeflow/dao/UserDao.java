package shoeflow.dao;

import shoeflow.model.User;

/** DAO untuk tabel users (+ nama role). Dipakai proses login. */
public interface UserDao {

    /** @return user (lengkap dengan nama role) atau null jika username tidak ditemukan. */
    User findByUsername(String username);
}
