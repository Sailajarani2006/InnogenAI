const GROQ_API_KEY = process.env.GROQ_API_KEY || "";

async function testGeneration() {
  console.log("Testing generation without response_format...");
  const startTime = Date.now();
  try {
    const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${GROQ_API_KEY}`
      },
      body: JSON.stringify({
        model: "openai/gpt-oss-120b",
        max_completion_tokens: 8192,
        messages: [
          {
            role: "system",
            content: "You are an expert full-stack developer. When given an idea, return a raw JSON object with keys: 'name', 'description', 'tags', 'readme', 'frontendCode', 'backendCode', 'databaseSchema'. Important: Output ONLY valid JSON starting with { and ending with }. All strings inside must be properly escaped JSON strings."
          },
          {
            role: "user",
            content: "Generate app for: ShopMate - modern e-commerce with cart and checkout"
          }
        ]
      })
    });
    
    console.log("HTTP Status:", response.status);
    const data = await response.json();
    if (data.error) {
      console.error("API Error:", data.error);
      return;
    }
    const text = data.choices[0].message.content;
    console.log("Received text length:", text.length, "Time:", (Date.now() - startTime)/1000, "s");

    // Clean JSON
    const firstBrace = text.indexOf('{');
    const lastBrace = text.lastIndexOf('}');
    if (firstBrace === -1 || lastBrace === -1) {
      throw new Error("No JSON structure found");
    }
    const jsonStr = text.substring(firstBrace, lastBrace + 1);
    const parsed = JSON.parse(jsonStr);
    console.log("SUCCESS! Parsed App Name:", parsed.name);
    console.log("Frontend code size:", parsed.frontendCode?.length);
    console.log("Backend code size:", parsed.backendCode?.length);
    console.log("Database schema size:", parsed.databaseSchema?.length);
  } catch (err) {
    console.error("Failed:", err.message);
  }
}

testGeneration();
