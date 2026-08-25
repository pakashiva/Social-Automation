eval_data = [

    {
        "query": "AI & Technology Education",
        "expected_contexts": [
            "Pillar 1 — AI & Technology Education",
            "Allocation: 20%",
            "Objective: Explain complicated technology in simple language that business owners, founders, operators, and technology leaders can understand.",
            "Example Topics: What exactly is an AI agent?; AI Agent vs Chatbot; What is RAG?; What is an API?; What happens when an OTP is sent?; What does an LLM actually do?; What is vector search?; AI workflow vs traditional automation; What is MCP?; What does agentic AI actually mean?; Where should businesses use AI?; Where should businesses NOT use AI?; What is an embedding?; What is a vector database?; What is workflow orchestration?; AI assistant vs AI agent; What is human-in-the-loop AI?; Why do AI systems hallucinate?",
            "Recommended Structure: HOOK → A simple question or misconception; CONCEPT → Explain it without jargon; EXAMPLE → Use a normal business situation; WHY IT MATTERS → Connect technology to business; TAKEAWAY → One memorable idea.",
            "Example: An AI Agent and a Chatbot aren't the same thing. A chatbot answers. An agent can act. Imagine asking: Which customers haven't paid us this month? A chatbot might explain how to find them. An AI agent could check your database, identify overdue invoices, prepare reminders, and create follow-up tasks. That's the difference between AI that talks and AI that works."
        ],
    },

    {
        "query": "Business Problems",
        "expected_contexts": [
            "Pillar 2 — Business Problems",
            "Allocation: 15%",
            "Objective: Describe operational problems businesses regularly face in a way that makes readers think: We have exactly this problem.",
            "Example Topics: Why Excel becomes difficult as companies grow; Information trapped in WhatsApp; Duplicate customer data; Manual approvals; Employees maintaining the same information in multiple places; No single source of truth; Manual reporting; Forgotten customer follow-ups; Business knowledge existing only in someone's head; Departments operating in silos; Software employees do not actually use; Reports arriving too late to be useful; Re-entering the same data into multiple systems; Processes that depend entirely on one employee; Managers lacking real-time visibility.",
            "Recommended Structure: OBSERVATION → PAIN → WHY IT HAPPENS → BETTER WAY OF THINKING → TAKEAWAY / QUESTION.",
            "Example: If your business stops working when one employee goes on leave, you may not have a people problem. You might have a process problem. Maybe only one person knows which spreadsheet is updated. Only one person knows which customers need follow-up. Only one person knows how the monthly report is prepared. Good systems don't replace good employees. They make sure knowledge doesn't disappear when someone isn't available."
        ],
    },

    {
        "query": "Digital Transformation",
        "expected_contexts": [
            "Pillar 3 — Digital Transformation",
            "Allocation: 15%",
            "Objective: Help businesses understand what genuine digital transformation means.",
            "Example Topics: Digitization vs digitalization vs digital transformation; Buying software is not digital transformation; Why software adoption fails; Why processes should be fixed before digitizing them; Manual → digital → automated → intelligent; What SMEs should digitize first; Why adding more software does not necessarily solve problems; Creating a single source of truth; Data-driven decision making; Why disconnected systems create operational problems; Why digital transformation is primarily a process problem.",
            "Example: Digitization is not converting your register into Excel. Paper register → Excel is digitization. Excel → connected system is digitalization. Connected system → automated workflows is transformation. Automated workflows → systems that can reason and act is where AI starts becoming interesting. Many businesses are trying to jump from Step 1 to Step 4."
        ],
    },

    {
        "query": "Automation Ideas",
        "expected_contexts": [
            "Pillar 4 — Automation Ideas",
            "Allocation: 15%",
            "Objective: Show businesses practical workflows that can be automated.",
            "Rule: Avoid vague statements such as AI can automate customer support. Show the workflow.",
            "Example Topics: Five things a business can automate this week; Customer follow-ups; Invoice reminders; Daily reports; Lead assignment; Support ticket classification; Employee onboarding; Lead qualification; Customer support automation; Invoice processing; Approval workflows; Report generation; Document processing; Appointment scheduling; Customer feedback collection; Data synchronization; Notification and escalation workflows.",
            "Recommended Structure: Customer sends message → AI understands request → Checks customer/account → Answers simple issue → Creates ticket if required → Assigns correct team → Updates CRM.",
            "Rule: Never discuss automation without making the workflow understandable."
        ],
    },

    {
        "query": "Builder / Founder Insights",
        "expected_contexts": [
            "Pillar 5 — Builder / Founder Insights",
            "Allocation: 10%",
            "Objective: Share genuine observations and lessons ELVA learns while building technology and working with businesses.",
            "Rule: This should not become generic startup motivation.",
            "Example Topics: What we learn talking to traditional businesses; Why requirements change after customers see software; Why understanding a workflow is harder than coding it; What building software for different industries teaches us; Why founders should not automate broken processes; Why customers describe symptoms instead of requirements; Why simple software is difficult to build; What implementing AI in real workflows teaches us; Why software requirements often hide deeper business problems; What we learn from watching users actually use software.",
            "Example: One thing building software for businesses has taught us: Customers rarely describe the actual problem. They'll say: We need a report. But when you keep asking why, the real problem might be: I don't know what's happening in my business until the end of the month. The report isn't the requirement. Visibility is. Understanding that difference is often harder than writing the code.",
            "Rule: The agent must never invent founder experiences. Builder posts must come from approved ELVA knowledge, documented experiences, or human-provided context."
        ],
    },

    {
        "query": "Industry Technology",
        "expected_contexts": [
            "Pillar 6 — Industry Technology",
            "Allocation: 10%",
            "Objective: Explain how technology affects real industries. This differentiates ELVA from generic AI pages.",
            "Example Topics: Dairy — How milk procurement works; Dairy — FAT/SNF concepts; Dairy — Digital payments to farmers; Dairy — Milk traceability; Education — Attendance data; Education — Fee management; Education — Student lifecycle management; Education — Parent communication; Retail — Inventory visibility; Retail — Hyperlocal commerce; Retail — Why local stores struggle with digital adoption; Retail — Customer retention; Manufacturing — Production tracking; Manufacturing — Quality control; Manufacturing — Inventory; Manufacturing — Predictive maintenance."
        ],
    },

    {
        "query": "Opinions & Discussions",
        "expected_contexts": [
            "Pillar 7 — Opinions & Discussions",
            "Allocation: 10%",
            "Objective: Publish thoughtful, defensible opinions that encourage discussion without manufacturing controversy.",
            "Example Topics: Every business does not need AI.; Excel is not bad software; businesses often ask it to do jobs it was never designed for.; Buying an ERP does not digitally transform a company.; AI agents will not fix broken processes.; The best business software is often software employees barely notice.; SMEs may not have a data problem; they may have a data-organization problem.; Automation should not always begin with AI.; More software does not necessarily mean better operations.; A dashboard is useless if the underlying data is unreliable.; The best automation sometimes removes a step rather than automating it.",
            "Rule: Opinions must be defensible, relevant to ELVA's expertise, useful, respectful, and supported by reasoning. Avoid generic engagement bait such as Agree?"
        ],
    },

    {
        "query": "Tech Behind Everyday Things",
        "expected_contexts": [
            "Pillar 8 — Tech Behind Everyday Things",
            "Allocation: 5%",
            "Objective: Explain the invisible technology behind everyday digital experiences.",
            "Example Topics: What actually happens when you receive an OTP?; Scan a QR code?; Click Pay Now?; Place an online order?; Use Google login?; Receive a WhatsApp notification?; Search for something?; Reset your password?; Make a UPI payment?; Upload a file?; Use a URL shortener?; Receive an email notification?",
            "Example: You scan a QR code and ₹500 reaches a shopkeeper. It feels instantaneous. Behind that simple action, multiple systems have to identify you, identify the merchant, authenticate the transaction, communicate with banks, validate the payment, and confirm its status. Great technology often feels boring to the user. That's usually the point."
        ],
    },

]