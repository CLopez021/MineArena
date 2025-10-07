<p align="center"><img src="./images/mine_arena_logo.png" alt="Logo" width="200"></p>
<h1 align="center">
	Mine Arena
</h1>
<p>A Forge mod built for Minecraft Java 1.21.1 that makes for voice controlled spell casting (like in Mage Arena) and additionally allows for the creation for custom spells using natural language prompt.</p>
<p align="center">
  <h1>Demo Video</h1>
  <a href="https://www.youtube.com/watch?v=4HqonoI8u2E">
    <img src="https://img.youtube.com/vi/4HqonoI8u2E/maxresdefault.jpg" width="600" />
  </a>
</p>

## 📋 Requirements

### Essential
- **Minecraft Java Edition 1.21.1** with Forge installed
- **Compatible Web Browser** with Speech Recognition API support:
  - ✅ Google Chrome (recommended)
  - ✅ Microsoft Edge
  - ✅ Safari
  - ❌ Firefox (not supported)

### Optional (For Creating Custom Spells)
- **<a href="https://openrouter.ai/">OpenRouter Account</a>** - Free to create, and free to use as long as you use the free models under this <a href="https://openrouter.ai/models?max_price=0">list</a>.
- **<a href="https://www.meshy.ai/">Meshy AI Subscription</a>** - Paid subscription required for 3D model generation API access

<p><strong>Note:</strong> The mod comes with 5 default spells that work immediately without any API keys. You only need OpenRouter and Meshy accounts if you want to create your own custom spells.</p>

## ✨ Core Features

### 🎤 Voice-Controlled Spell Casting
Cast spells using your voice! Simply speak the spell's cast phrase to launch fireballs, summon ice storms, create shockwaves, or whatever else you'd like. Voice recognition automatically activates when you join a world by opening up a new prompt on the supported browsers and requesting permission to use your microphone.

### 🧙 AI-Powered Custom Spell Creation
Create your own custom spells with natural language:
1. Right-click with the **Wand** item (craftable in-game)
2. Describe your spell idea (e.g., "A lightning bolt that stuns enemies")
3. Set a voice command phrase to cast it (e.g., "Thunder Strike")
4. The AI generates the spell's behavior, effects, and visual appearance
5. Cast it by speaking your custom phrase!

**Powered by:**
- **OpenRouter LLM** - Generates spell mechanics (damage, effects, behavior)
- **Meshy AI** - Creates unique 3D models for spell projectiles

### 🔮 Default Spells
The mod includes five ready-to-use spells (no API keys required):
- **Infernal Blast** (say "Fireball") - Explosive fireball with area damage and ignite effect
- **Gale Force** (say "Wind") - Wind blast that knocks back enemies
- **Glacial Prison** (say "Ice cube") - Ice burst that freezes targets and places ice blocks
- **Arcane Detonation** (say "Bomb") - High-speed explosive bomb with block destruction
- **Ethereal Ascension** (say "Levitate") - Self-cast levitation spell


<h2>How to Install/Compile</h2>
<p>To install this mod, simply copy the <code>mine_arena-1.0.0.jar</code> file into the mods folder of your Minecraft directory.
</p>
<p>If you would like to compile the .jar file yourself, navigate to the root directory of the project and run <code>gradlew build</code> from the command line.</p>

<h2>Setup & Configuration</h2>

<h3>Required API Keys</h3>
<p>To use the AI spell creation features, you'll need API keys from two services:</p>

<h4>1. OpenRouter (Required for spell generation)</h4>
<ul>
    <li>Sign up at <a href="https://openrouter.ai/">OpenRouter</a></li>
    <li>Add credits to your account (pays for LLM API calls)</li>
    <li>Copy your API key</li>
    <li>Open <code>config/mine_arena-common.toml</code> in your Minecraft directory</li>
    <li>Paste your OpenRouter API key in the config file</li>
</ul>

<h4>2. Meshy AI (Required for spell visuals)</h4>
<ul>
    <li>Create an account at <a href="https://www.meshy.ai/settings/api">Meshy API Page</a></li>
    <li>Purchase a subscription to get API access</li>
    <li>Copy your API key</li>
    <li>Paste your Meshy API key in <code>config/mine_arena-common.toml</code></li>
</ul>

<p><strong>Note:</strong> Default spells work without API keys. API keys are only needed to create custom spells.</p>

<h2>How to Use</h2>

<h3>🎮 Casting Spells with Voice</h3>
<ol>
    <li>Join your world (voice recognition auto-starts)</li>
    <li>Speak a spell's cast phrase clearly (e.g., "Fireball" for Infernal Blast)</li>
    <li>The spell will be cast in the direction you're looking!</li>
</ol>

<p><strong>Tip:</strong> The mod uses your browser's speech recognition. A browser tab will open when you join - keep it open in the background.</p>

<h3>🧪 Creating Custom Spells</h3>
<ol>
    <li><strong>Craft a Wand</strong>: Use 1 gold ingot + 2 sticks in a vertical pattern (stick-gold-stick)</li>
    <li>Right-click with the Wand to open the spell creation interface</li>
    <li>Enter a description of your spell (be creative! e.g., "A poison cloud that damages enemies over time")</li>
    <li>Enter a cast phrase (the words you'll speak to cast it)</li>
    <li>Click Submit and wait for the AI to generate your spell (may take 1-2 minutes)</li>
    <li>Once complete, speak your cast phrase to use it!</li>
</ol>

<p><strong>Crafting Recipe:</strong></p>
<pre>
    S  (Stick)
    G  (Gold Ingot)
    S  (Stick)
</pre>

<h3>Browser Compatibility</h3>
<p>Voice recognition requires a browser with Web Speech API support:</p>
<ul>
    <li>✅ Chrome (recommended)</li>
    <li>✅ Edge</li>
    <li>✅ Safari</li>
    <li>❌ Firefox (no Web Speech API support)</li>
</ul>

<h2>Examples of Custom Spell Prompts</h2>

<p>Here are some ideas for custom spells you can create:</p>

<ul>
    <li><strong>"A healing aura around me"</strong>
    <li><strong>"Summon wolves to fight for me"</strong> 
    <li><strong>"A meteor that destroys terrain"</strong>
    <li><strong>"Poison gas cloud"</strong>
</ul>

<h2>Troubleshooting</h2>

<h3>Voice Recognition Not Working</h3>
<ul>
    <li>Make sure the browser tab that opens stays open in the background</li>
    <li>Check that your browser has microphone permissions</li>
    <li>Try using Chrome or Edge (best compatibility)</li>
    <li>Speak clearly and use simple, distinct cast phrases</li>
</ul>

<h3>Spell Generation Fails</h3>
<ul>
    <li>Verify both API keys are correctly set in <code>config/mine_arena-common.toml</code></li>
    <li>Check that you have credits in your OpenRouter and Meshy accounts</li>
    <li>Try simplifying your spell description</li>
    <li>Wait a moment and try again (API rate limits may apply)</li>
</ul>

<h3>Spells Not Casting</h3>
<ul>
    <li>Make sure voice recognition is active (browser tab open)</li>
    <li>Check that your spell has been successfully created (open the Wand interface to see your spell list)</li>
    <li>Try speaking the cast phrase more clearly or slowly</li>
    <li>Verify the cast phrase doesn't conflict with other spells</li>
</ul>

<h2>License</h2>

<p>This project is open source. Feel free to modify and use it for your own projects.</p>