from agents.topic_evaluator.eval_functions import generate_topic
from rag_system.rag_functions import retrieve_semantic_chunks
from agents.content_writer_agent.content_functions import generate_linkedin_content
from publisher.facebook_functions import publish_to_facebook


output = generate_topic()
# this is yet to be figured
data = retrieve_semantic_chunks(output.pillar)
post = generate_linkedin_content(pillar=output.pillar , topic=output.topic , 
                                 post_format=output.post_format , brand_voice=output.brand_voice,
                                 pillar_guidlines=data)

publish_to_facebook(post.content)


