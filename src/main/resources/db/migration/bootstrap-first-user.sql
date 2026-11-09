-- Primera cuenta de una instalacion nueva. Ejecutar UNA sola vez.
--
-- El hash de ejemplo corresponde a la contrasenya "cambiame-ahora-mismo".
-- Generalo tu con Argon2id antes de usarlo en produccion; por ejemplo con un
-- test de una sola linea:
--
--   System.out.println(Argon2PasswordEncoder
--       .defaultsForSpringSecurity_v5_8().encode("tu-contrasenya"));
--
-- Nunca dejes el hash de ejemplo: esta publicado en el repositorio.

INSERT INTO app_user (email, password_hash, display_name, enabled, created_at, updated_at)
VALUES (
    'yourmail@pm.me',
    '$argon2id$v=19$m=16384,t=2,p=1$YourPassword$YourPassword',
    'Your Name',
    TRUE,
    NOW(6),
    NOW(6)
);

INSERT INTO space (name, max_members, created_at, updated_at)
VALUES ('Nuestras listas', 2, NOW(6), NOW(6));

INSERT INTO space_member (space_id, user_id, role, joined_at)
SELECT s.id, u.id, 'OWNER', NOW(6)
FROM space s, app_user u
WHERE s.name = 'Nuestras listas' AND u.email = 'yourmail@pm.me';

-- A partir de aqui, entra en la aplicacion, cambia la contrasenya y crea una
-- invitacion para tu pareja desde POST /api/spaces/{id}/invitations.
