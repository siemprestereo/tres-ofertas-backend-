CREATE TABLE banned_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT banned_words_word_unique UNIQUE (word)
);

INSERT INTO banned_words (word) VALUES
('pelotudo'), ('pelotuda'), ('boludo'), ('boluda'), ('forro'), ('forra'),
('garca'), ('puta'), ('puto'), ('prostituta'), ('mierda'), ('cagada'),
('culo'), ('concha'), ('pija'), ('verga'), ('chota'), ('hdp'),
('hijo de puta'), ('hija de puta'), ('la concha'), ('la puta'),
('carajo'), ('cagón'), ('cagon'), ('inutil'), ('inútil'),
('estúpido'), ('estupido'), ('estúpida'), ('estupida'),
('idiota'), ('imbecil'), ('imbécil'), ('bastardo'), ('bastarda'),
('mogolico'), ('mogólico'), ('retrasado'), ('retrasada'),
('tarado'), ('tarada'), ('cretino'), ('cretina');
