-- ShoeFlow: DATA AWAL (hanya INSERT, BUKAN struktur tabel). Jalankan SEKALI di phpMyAdmin pada database `shoeflow`.
USE shoeflow;

-- Akun login (password disimpan apa adanya; aplikasi juga mendukung hash SHA-256 hex)
INSERT IGNORE INTO users (role_id, name, username, password) VALUES
((SELECT id FROM roles WHERE name='admin'),   'Administrator', 'admin',   'admin123'),
((SELECT id FROM roles WHERE name='manajer'), 'Manajer Toko',  'manajer', 'manajer123'),
((SELECT id FROM roles WHERE name='kasir'),   'Kasir 1',       'kasir',   'kasir123');

-- Supplier contoh
INSERT INTO suppliers (name, phone, address) VALUES
('PT Sepatu Jaya Abadi', '021-5551234', 'Jl. Industri No. 10, Bekasi'),
('CV Langkah Maju',      '022-7774321', 'Jl. Cibaduyut No. 5, Bandung');

-- Produk contoh (gambar boleh dikosongkan; bisa diunggah lewat menu Manajemen Produk)
INSERT INTO products (brand, model, price) VALUES
('Nike',     'Air Max 90',       1899000),
('Adidas',   'Ultraboost',       2499000),
('Converse', 'Chuck 70',         1299000),
('Puma',     'Suede Classic',    1099000);

-- Varian contoh (SKU unik). Stok sebagian sengaja <= 2 untuk mencoba alert Dashboard.
INSERT INTO variants (product_id, size, color, sku, stock, min_stock) VALUES
((SELECT id FROM products WHERE model='Air Max 90'),     '41', 'Hitam', 'NIK-AM90-41-HTM', 10, 2),
((SELECT id FROM products WHERE model='Air Max 90'),     '42', 'Hitam', 'NIK-AM90-42-HTM',  2, 2),
((SELECT id FROM products WHERE model='Air Max 90'),     '42', 'Putih', 'NIK-AM90-42-PTH',  6, 2),
((SELECT id FROM products WHERE model='Ultraboost'),     '40', 'Abu',   'ADI-UB-40-ABU',    5, 2),
((SELECT id FROM products WHERE model='Ultraboost'),     '43', 'Hitam', 'ADI-UB-43-HTM',    1, 2),
((SELECT id FROM products WHERE model='Chuck 70'),       '39', 'Putih', 'CON-C70-39-PTH',   8, 2),
((SELECT id FROM products WHERE model='Chuck 70'),       '41', 'Hitam', 'CON-C70-41-HTM',   7, 2),
((SELECT id FROM products WHERE model='Suede Classic'),  '42', 'Merah', 'PUM-SUE-42-MRH',   4, 2);
