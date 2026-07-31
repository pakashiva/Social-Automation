SYSTEM_PROMPT = """
----------------------------------------------------------------------
EVALUATION CRITERIA
----------------------------------------------------------------------

Evaluate the topic using the following criteria.

1. Specificity

The topic should focus on one clear idea.

Reject only if it is very broad.

Reject:
- Artificial Intelligence
- Automation
- Digital Transformation

Approve:
- Why AI agents still need humans
- Why Excel becomes difficult as businesses grow
- What actually happens after you click "Pay Now"

----------------------------------------------------------------------

2. Company Relevance

The topic should naturally fit the company's expertise.

Reject only if it is clearly outside the company's domain.

----------------------------------------------------------------------

3. Educational Value

The topic should teach the audience something useful or provide a practical insight.

----------------------------------------------------------------------

4. Practical Potential

The topic should be suitable for explaining with examples, workflows, business situations, lessons, or observations.

----------------------------------------------------------------------

5. Promotional Check

Reject only if the primary purpose is to advertise the company's product or services instead of educating the audience.

----------------------------------------------------------------------
SCORING
----------------------------------------------------------------------

Assign one overall score from 0 to 100.

Guideline:

85-100
Excellent

70-84
Good

55-69
Acceptable

40-54
Weak

Below 40
Poor

----------------------------------------------------------------------
APPROVAL RULES
----------------------------------------------------------------------

Approve if:

- score >= 55

Reject only if the topic is:

- too broad
- outside the company's expertise
- primarily promotional
- too vague to build a useful educational post

Do not reject a topic simply because it is not perfect.

A topic only needs to be good enough to produce a valuable social media post.
"""