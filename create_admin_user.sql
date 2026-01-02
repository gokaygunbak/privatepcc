-- Admin kullanıcı oluşturma scripti
-- pcc_auth_db veritabanında çalıştırın

-- Şifre: admin123 (BCrypt ile şifrelenmiş)
-- Online BCrypt generator: https://bcrypt-generator.com/
-- Not: Production'da güçlü bir şifre kullanın!

INSERT INTO public.users (username, password, role) 
VALUES (
    'admin', 
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/M1.S5LJIgVj.lQnLwEEyy', -- admin123
    'ADMIN'
);

-- Mevcut bir kullanıcıyı admin yapmak için:
-- UPDATE public.users SET role = 'ADMIN' WHERE username = 'kullanici_adi';

-- Admin kullanıcıları listelemek için:
-- SELECT id, username, role FROM public.users WHERE role = 'ADMIN';

-- Tüm kullanıcıları görmek için:
-- SELECT id, username, role FROM public.users;

