USE moviedb;

-- Drop the existing procedure
DROP PROCEDURE IF EXISTS add_movie;

DELIMITER //
CREATE PROCEDURE add_movie(
    IN movie_title VARCHAR(100),
    IN movie_year INTEGER,
    IN movie_director VARCHAR(100),
    IN star_name VARCHAR(100),
    IN genre_name VARCHAR(32)
)
BEGIN
   DECLARE movie_id VARCHAR(10);
   DECLARE star_id VARCHAR(10);
   DECLARE genre_id INT;
   DECLARE existing_movie_id VARCHAR(10);
   DECLARE movie_price DECIMAL(10,2);

   -- Calculate random price between 5 and 20
   SET movie_price = 5 + (RAND() * 15);

   -- First check if movie exists
SELECT id INTO existing_movie_id
FROM movies
WHERE title = movie_title
          AND year = movie_year
  AND director = movie_director;

IF existing_movie_id IS NOT NULL THEN
SELECT CONCAT('Movie already exists: ', movie_title, ' (ID: ', existing_movie_id, ')') as message;
ELSE
       -- Generate new movie ID
SELECT CONCAT('tt', LPAD(SUBSTRING(MAX(id), 3) + 1, 7, '0'))
INTO movie_id
FROM movies;

-- Insert the movie with price
INSERT INTO movies (id, title, year, director, price)
VALUES (movie_id, movie_title, movie_year, movie_director, movie_price);

-- Handle star (find existing or create new)
SELECT id INTO star_id
FROM stars
WHERE name = star_name
    LIMIT 1;  -- In case of multiple stars with same name, link to any

IF star_id IS NULL THEN
           -- Generate new star ID
SELECT CONCAT('nm', LPAD(SUBSTRING(MAX(id), 3) + 1, 7, '0'))
INTO star_id
FROM stars;

-- Insert new star
INSERT INTO stars (id, name)
VALUES (star_id, star_name);

SELECT CONCAT('Created new star: ', star_name, ' (ID: ', star_id, ')') as message;
ELSE
SELECT CONCAT('Using existing star: ', star_name, ' (ID: ', star_id, ')') as message;
END IF;

-- Handle genre (find existing or create new)
SELECT id INTO genre_id
FROM genres
WHERE name = genre_name;

IF genre_id IS NULL THEN
    -- Insert new genre
    INSERT INTO genres (name)
    VALUES (genre_name);
            
    SET genre_id = LAST_INSERT_ID();
    SELECT CONCAT('Created new genre: ', genre_name, ' (ID: ', genre_id, ')') as message;
ELSE
    SELECT CONCAT('Using existing genre: ', genre_name, ' (ID: ', genre_id, ')') as message;
END IF;

       -- Create movie-star relationship
INSERT INTO stars_in_movies (starId, movieId)
VALUES (star_id, movie_id);

-- Create movie-genre relationship
INSERT INTO genres_in_movies (genreId, movieId)
VALUES (genre_id, movie_id);

SELECT CONCAT('Successfully added movie: ', movie_title, ' (ID: ', movie_id, ')') as message;
END IF;
END //
DELIMITER ;