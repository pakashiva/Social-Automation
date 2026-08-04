from app import db

class PublishedPost(db.Model):

    __bind_key__ = "published"
    __tablename__ = "published_posts"


    id = db.Column(db.Integer, primary_key=True)

    linkedin_post_id = db.Column(db.String(150))

    author_urn = db.Column(db.String(100))

    caption = db.Column(db.Text)

    image_url = db.Column(db.Text)

    platform = db.Column(db.String(20))

    published_at = db.Column(db.DateTime)