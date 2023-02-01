-- Oracle DB did not support GENERATE AS IDENTITY until version 12c.
-- It is not possible to alter a column to make it an identity column, so this
-- script creates triggers that emulates this behaviour.

CREATE OR REPLACE TRIGGER downloadtype_id_trigger
  BEFORE INSERT ON downloadtype
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/

CREATE OR REPLACE TRIGGER cartitem_id_trigger
  BEFORE INSERT ON cartitem
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/

CREATE OR REPLACE TRIGGER downloaditem_id_trigger
  BEFORE INSERT ON downloaditem
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/

CREATE OR REPLACE TRIGGER download_id_trigger
  BEFORE INSERT ON download
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/

CREATE OR REPLACE TRIGGER cart_id_trigger
  BEFORE INSERT ON cart
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/

CREATE OR REPLACE TRIGGER parententity_id_trigger
  BEFORE INSERT ON parententity
  FOR EACH ROW
BEGIN
  :new.id := seq_gen_identity.nextval;
END;
/
