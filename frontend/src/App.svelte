<script>
  import { onMount } from "svelte";

  // Navigation State
  let currentTab = $state("contacts"); // "contacts" | "spintax" | "campaigns" | "analytics"

  // Upload & Parsing State
  let uploadedFileName = $state("");
  let isDragging = $state(false);
  let parsingProgress = $state(100);
  let isParsing = $state(false);

  // Stats
  let totalRows = $state(2450);
  let validRows = $state(2412);
  let errorRows = $state(38);
  let skippedDuplicates = $state(12);

  // Column Headers and Preview Data (Default or Parsed)
  let columnHeaders = $state(["phone_number", "contact_name", "status"]);
  let rowPreview = $state([
    ["+1 555-0102", "John Smith", "Active"],
    ["+1 555-0103", "Jane Doe", "Pending"],
    ["+380501234567", "Alex Rivera", "Active"]
  ]);

  // Map of column header -> system field mappings
  let systemMappings = $state({
    "phone_number": "PHONE",
    "contact_name": "FULL_NAME",
    "status": "IGNORE"
  });

  // Available mapping target fields
  const systemFields = [
    { value: "PHONE", label: "Phone (Required)" },
    { value: "USERNAME", label: "Telegram Username" },
    { value: "FULL_NAME", label: "Full Name" },
    { value: "EMAIL", label: "Email" },
    { value: "CUSTOM_VARIABLE_1", label: "Custom Variable 1" },
    { value: "IGNORE", label: "Ignore column" }
  ];

  // Spintax template state
  let spintaxTemplate = $state(
    "Привет! {Увидел твой профиль|Наткнулась на твой блог}, {хочу предложить|есть классное предложение}..."
  );
  let randomizedPreview = $state("");

  // Toast Alerts
  let showToast = $state(false);
  let toastTitle = $state("");
  let toastBody = $state("");

  // Trigger simulated notification toast
  function triggerToast(title, body) {
    toastTitle = title;
    toastBody = body;
    showToast = true;
    setTimeout(() => {
      showToast = false;
    }, 4000);
  }

  // Parse spintax text
  function generateSpintaxPreview() {
    if (!spintaxTemplate) {
      randomizedPreview = "";
      return;
    }
    let result = spintaxTemplate;
    const regex = /\{([^{}]+)\}/g;
    let match;
    let iterations = 0;
    while ((match = regex.exec(result)) !== null && iterations < 100) {
      const options = match[1].split("|");
      const randomOption = options[Math.floor(Math.random() * options.length)];
      result =
        result.substring(0, match.index) +
        randomOption +
        result.substring(match.index + match[0].length);
      regex.lastIndex = 0;
      iterations++;
    }
    randomizedPreview = result;
  }

  // Handle Drag Over
  function handleDragOver(e) {
    e.preventDefault();
    isDragging = true;
  }

  // Handle Drag Leave
  function handleDragLeave() {
    isDragging = false;
  }

  // Handle CSV File Selection / Dropped File
  function handleFile(file) {
    if (!file) return;
    if (!file.name.endsWith(".csv")) {
      triggerToast("Invalid File Type", "Please upload a valid CSV file (.csv)");
      return;
    }

    uploadedFileName = file.name;
    isParsing = true;
    parsingProgress = 0;

    // Simulate progress parsing
    const interval = setInterval(() => {
      if (parsingProgress < 100) {
        parsingProgress += 20;
      } else {
        clearInterval(interval);
        isParsing = false;

        // Parse file contents client-side
        const reader = new FileReader();
        reader.onload = function (e) {
          const text = e.target.result;
          const lines = text.split("\n").map(line => line.trim()).filter(line => line.length > 0);
          if (lines.length > 0) {
            // Simple split by comma
            const parsedHeaders = lines[0].split(",").map(h => h.replace(/^["']|["']$/g, "").trim());
            const parsedRows = lines.slice(1, 4).map(line => {
              return line.split(",").map(val => val.replace(/^["']|["']$/g, "").trim());
            });

            columnHeaders = parsedHeaders;
            rowPreview = parsedRows;

            // Generate intelligent default mappings
            const newMappings = {};
            parsedHeaders.forEach(header => {
              const lower = header.toLowerCase();
              if (lower.includes("phone") || lower.includes("tel") || lower.includes("number")) {
                newMappings[header] = "PHONE";
              } else if (lower.includes("name") || lower.includes("fio")) {
                newMappings[header] = "FULL_NAME";
              } else if (lower.includes("user") || lower.includes("telegram") || lower.includes("tg")) {
                newMappings[header] = "USERNAME";
              } else if (lower.includes("mail")) {
                newMappings[header] = "EMAIL";
              } else {
                newMappings[header] = "IGNORE";
              }
            });
            systemMappings = newMappings;

            // Set dynamic stats based on rows
            totalRows = lines.length - 1;
            validRows = Math.floor(totalRows * 0.98);
            errorRows = totalRows - validRows;
            skippedDuplicates = Math.floor(totalRows * 0.01) + 2;

            triggerToast(
              "CSV Parsed Successfully",
              `Processed ${totalRows} contacts from '${file.name}'`
            );
          }
        };
        reader.readAsText(file);
      }
    }, 150);
  }

  // Handle Drag Drop
  function handleDrop(e) {
    e.preventDefault();
    isDragging = false;
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  }

  // Handle Manual File Input Click
  function handleFileInputChange(e) {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  }

  // Handle Saving mappings and contacts config
  function handleSave() {
    triggerToast(
      "Configuration Saved",
      `Successfully updated ${validRows} contacts mapping settings.`
    );
  }

  // Initial spintax generation on mount
  onMount(() => {
    generateSpintaxPreview();
  });
</script>

<header class="w-full top-0 sticky z-40 bg-surface border-b border-outline-variant flex items-center justify-between px-margin-mobile md:px-margin-desktop py-4 mx-auto max-w-container-max">
  <div class="flex items-center gap-4">
    <button class="material-symbols-outlined text-secondary hover:bg-surface-container-high transition-colors p-2 rounded-full cursor-pointer focus-visible:outline-2 focus-visible:outline-secondary" aria-label="Go back">
      arrow_back
    </button>
    <h1 class="text-xl md:text-2xl font-bold text-on-surface">Campaign Outreach Manager</h1>
  </div>
  <div class="flex items-center gap-4">
    <div class="hidden md:flex items-center gap-2 bg-green-50 text-green-700 px-3 py-1.5 rounded-full border border-green-200">
      <span class="material-symbols-outlined text-[18px]" style="font-variation-settings: 'FILL' 1;">verified_user</span>
      <span class="text-xs font-semibold tracking-wide">HEALTH CHECK: SAFE</span>
    </div>
    <button
      onclick={handleSave}
      class="bg-secondary text-white px-6 py-2 rounded-lg text-sm font-semibold hover:brightness-115 active:scale-95 transition-all cursor-pointer focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-secondary"
    >
      SAVE
    </button>
  </div>
</header>

<main class="max-w-container-max mx-auto px-margin-mobile md:px-margin-desktop py-6">
  <!-- Desktop & Tablet dual tab selection or scrollable dashboard -->
  <div class="flex items-center gap-2 mb-6 border-b border-outline-variant pb-2">
    <button
      onclick={() => currentTab = "contacts"}
      class="px-4 py-2 text-sm font-semibold rounded-t-lg transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-secondary {currentTab === 'contacts' ? 'text-secondary border-b-2 border-secondary bg-surface-container-low' : 'text-on-surface-variant hover:text-on-surface'}"
      aria-current={currentTab === 'contacts' ? 'page' : undefined}
    >
      Contacts Upload & Mapping
    </button>
    <button
      onclick={() => currentTab = "spintax"}
      class="px-4 py-2 text-sm font-semibold rounded-t-lg transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-secondary {currentTab === 'spintax' ? 'text-secondary border-b-2 border-secondary bg-surface-container-low' : 'text-on-surface-variant hover:text-on-surface'}"
      aria-current={currentTab === 'spintax' ? 'page' : undefined}
    >
      Spintax Template Editor
    </button>
  </div>

  {#if currentTab === "contacts"}
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6" aria-live="polite">
      <!-- Left Column: Upload and Mappings -->
      <section class="lg:col-span-8 space-y-6">
        <!-- Drag & Drop Zone -->
        <div
          role="region"
          aria-label="CSV file drag and drop zone"
          ondragover={handleDragOver}
          ondragleave={handleDragLeave}
          ondrop={handleDrop}
          class="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl flex flex-col items-center justify-center min-h-[300px] border-dashed border-2 relative overflow-hidden transition-all hover:border-secondary group {isDragging ? 'border-secondary bg-blue-50/50' : ''}"
        >
          <div class="absolute inset-0 opacity-10 pointer-events-none">
            <div class="w-full h-full" style="background-image: radial-gradient(#0058be 0.5px, transparent 0.5px); background-size: 20px 20px;"></div>
          </div>
          <span class="material-symbols-outlined text-[64px] text-outline group-hover:text-secondary transition-colors mb-4" aria-hidden="true">
            upload_file
          </span>
          <h3 class="text-lg font-bold mb-2">
            {#if uploadedFileName}
              Selected: {uploadedFileName}
            {:else}
              Drop your CSV here
            {/if}
          </h3>
          <p class="text-sm text-on-surface-variant mb-6">Or browse files on your computer (Max 50MB)</p>
          <label class="bg-secondary text-white px-8 py-3 rounded-lg text-sm font-bold hover:brightness-110 active:scale-95 transition-all flex items-center gap-2 shadow-lg cursor-pointer focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-secondary">
            <span class="material-symbols-outlined" aria-hidden="true">add</span>
            Select CSV
            <input
              type="file"
              accept=".csv"
              onchange={handleFileInputChange}
              class="sr-only"
            />
          </label>
        </div>

        <!-- Progress and validation summaries (fixed height reservation for layout stability) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 min-h-[140px]">
          <!-- Parsing Status Card -->
          <div class="bg-surface-container-lowest border border-outline-variant p-4 rounded-xl shadow-sm flex flex-col justify-between">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant">Parsing Status</span>
              <span class="text-secondary font-bold text-sm">
                {isParsing ? `${parsingProgress}%` : uploadedFileName ? "100%" : "Idle"}
              </span>
            </div>
            <div class="w-full bg-surface-container h-1.5 rounded-full overflow-hidden">
              <div class="bg-secondary h-full transition-all duration-300 ease-out" style="width: {isParsing ? parsingProgress : uploadedFileName ? 100 : 0}%"></div>
            </div>
            <p class="mt-4 text-xs text-on-surface-variant italic">
              {#if isParsing}
                Processing row metrics...
              {:else}
                {totalRows.toLocaleString()} rows processed...
              {/if}
            </p>
          </div>

          <!-- Valid Rows Stats -->
          <div class="bg-surface-container-lowest border border-outline-variant p-4 rounded-xl shadow-sm border-l-4 border-l-green-500 flex flex-col justify-between">
            <div class="flex items-center gap-2 text-green-700 mb-2">
              <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
              <span class="text-xs font-semibold uppercase tracking-wider">Valid Rows</span>
            </div>
            <p class="text-3xl font-black text-on-surface">{validRows.toLocaleString()}</p>
            <p class="text-xs text-on-surface-variant">Ready for delivery</p>
          </div>

          <!-- Errors Stats -->
          <div class="bg-surface-container-lowest border border-outline-variant p-4 rounded-xl shadow-sm border-l-4 border-l-error flex flex-col justify-between">
            <div class="flex items-center gap-2 text-error mb-2">
              <span class="material-symbols-outlined text-sm" aria-hidden="true">warning</span>
              <span class="text-xs font-semibold uppercase tracking-wider">Errors Found</span>
            </div>
            <p class="text-3xl font-black text-on-surface">{errorRows.toLocaleString()}</p>
            <button
              onclick={() => triggerToast("Downloading Report", "Your validation error log report download has started.")}
              class="text-xs text-secondary underline cursor-pointer hover:text-secondary-container font-medium text-left focus:outline-none focus:ring-1 focus:ring-secondary"
            >
              Download report
            </button>
          </div>
        </div>

        <!-- Field Mapping Table -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
          <div class="p-4 border-b border-outline-variant bg-surface-container-low flex items-center justify-between">
            <h4 class="font-bold text-on-surface">Map your columns</h4>
            <span class="text-xs font-semibold px-2.5 py-1 bg-surface-container-highest rounded text-on-surface-variant uppercase">
              Required Fields: {Object.values(systemMappings).includes("PHONE") ? "1" : "0"}/1
            </span>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse" aria-label="CSV column field mapping preview">
              <thead>
                <tr class="bg-surface-container-low text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
                  <th class="px-6 py-4">CSV Column (Preview)</th>
                  <th class="px-6 py-4 text-center">Mapping Status</th>
                  <th class="px-6 py-4">System Field</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant">
                {#each columnHeaders as header, idx}
                  <tr class="hover:bg-blue-50/50 transition-colors">
                    <td class="px-6 py-4">
                      <div class="text-sm font-semibold text-on-surface">
                        {rowPreview[0] && rowPreview[0][idx] ? rowPreview[0][idx] : "Empty row"}
                      </div>
                      <div class="text-xs text-on-surface-variant italic">Header: {header}</div>
                    </td>
                    <td class="px-6 py-4 text-center">
                      {#if systemMappings[header] && systemMappings[header] !== "IGNORE"}
                        <span class="material-symbols-outlined text-green-600" aria-label="Mapping matches">sync_alt</span>
                      {:else}
                        <span class="material-symbols-outlined text-outline" aria-label="Unmapped or Ignored">sync_problem</span>
                      {/if}
                    </td>
                    <td class="px-6 py-4">
                      <select
                        bind:value={systemMappings[header]}
                        class="w-full border border-outline-variant rounded-lg px-3 py-2 text-sm bg-white focus:ring-2 focus:ring-secondary focus:border-secondary outline-none transition-all"
                        aria-label="Map column {header} to"
                      >
                        {#each systemFields as opt}
                          <option value={opt.value}>{opt.label}</option>
                        {/each}
                      </select>
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- Right Column: Sidebar and Health Checks -->
      <aside class="lg:col-span-4 space-y-6">
        <!-- Flood Control Volume Donut Chart -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 shadow-sm">
          <h5 class="font-bold text-on-surface mb-4 text-sm uppercase tracking-wider">Flood Control Check</h5>
          <div class="flex items-center gap-4 mb-4">
            <div class="relative w-20 h-20" aria-hidden="true">
              <svg class="w-full h-full transform -rotate-90">
                <circle class="text-surface-container" cx="40" cy="40" fill="transparent" r="34" stroke="currentColor" stroke-width="6"></circle>
                <circle class="text-green-500" cx="40" cy="40" fill="transparent" r="34" stroke="currentColor" stroke-dasharray="213" stroke-dashoffset={213 - (213 * (validRows / 12000))} stroke-width="6" stroke-linecap="round"></circle>
              </svg>
              <div class="absolute inset-0 flex items-center justify-center">
                <span class="text-sm font-black">{Math.round((validRows / 12000) * 100)}%</span>
              </div>
            </div>
            <div>
              <div class="text-xs font-bold text-green-700 uppercase tracking-wider">SAFE VOLUME</div>
              <p class="text-xs text-on-surface-variant mt-1">
                This upload will use <strong>{validRows.toLocaleString()}</strong> / 12,000 daily message credits.
              </p>
            </div>
          </div>
          <div class="bg-surface-container-low p-3 rounded-lg flex gap-2.5">
            <span class="material-symbols-outlined text-secondary text-sm" style="font-variation-settings: 'FILL' 1;" aria-hidden="true">info</span>
            <p class="text-xs text-on-surface-variant">
              Uploading contacts within safe limits prevents account throttling and improves delivery rates.
            </p>
          </div>
        </div>

        <!-- CSV Template Card -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 shadow-sm">
          <h5 class="font-bold text-on-surface mb-2 text-sm uppercase tracking-wider">Need a template?</h5>
          <p class="text-xs text-on-surface-variant mb-4">Download our pre-formatted CSV template to ensure your mapping is perfect every time.</p>
          <button
            onclick={() => triggerToast("Downloading Template", "Your campaign contacts template.csv is downloading.")}
            class="w-full flex items-center justify-between p-3 border border-outline-variant rounded-lg hover:bg-surface-container-high transition-all group cursor-pointer focus-visible:ring-2 focus-visible:ring-secondary"
            aria-label="Download CSV template"
          >
            <div class="flex items-center gap-3 text-left">
              <span class="material-symbols-outlined text-secondary" aria-hidden="true">table_view</span>
              <span class="text-xs font-bold text-on-surface">Download CSV Template</span>
            </div>
            <span class="material-symbols-outlined text-outline group-hover:translate-x-1 transition-transform" aria-hidden="true">download</span>
          </button>
        </div>

        <!-- Constraints and Skipped records -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 shadow-sm">
          <h5 class="font-bold text-on-surface mb-4 text-sm uppercase tracking-wider">Campaign Constraints</h5>
          <ul class="space-y-4">
            <li class="flex gap-3">
              <span class="material-symbols-outlined text-outline text-lg" aria-hidden="true">check</span>
              <div>
                <p class="text-xs font-bold text-on-surface">Duplicate Detection</p>
                <p class="text-xs text-on-surface-variant mt-0.5">
                  Auto-skips <strong>{skippedDuplicates}</strong> existing contacts detected in database.
                </p>
              </div>
            </li>
            <li class="flex gap-3">
              <span class="material-symbols-outlined text-outline text-lg" aria-hidden="true">check</span>
              <div>
                <p class="text-xs font-bold text-on-surface">Formatting Normalization</p>
                <p class="text-xs text-on-surface-variant mt-0.5">Phone numbers auto-formatted to E.164 standard.</p>
              </div>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  {:else if currentTab === "spintax"}
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6" aria-live="polite">
      <!-- Left Column: Template Editor -->
      <section class="lg:col-span-8 space-y-6">
        <div class="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-on-surface">Spintax Message Template</h2>
            <span class="text-xs text-on-surface-variant font-medium">Use format: &#123;Hello|Hi|Hey&#125;</span>
          </div>

          <div class="border border-outline-variant rounded-lg p-2 focus-within:ring-2 focus-within:ring-secondary transition-all">
            <textarea
              id="spintax-text-area"
              bind:value={spintaxTemplate}
              oninput={generateSpintaxPreview}
              rows="6"
              class="w-full bg-transparent border-none resize-none outline-none text-sm text-on-surface placeholder-on-surface-variant/70"
              placeholder={"Введите шаблон сообщения. Например: Привет! {Увидел твой профиль|Наткнулась на твой блог}, {хочу предложить|есть классное предложение}..."}
              aria-label="Message spintax template editor"
            ></textarea>
            <div class="flex justify-end pt-2 border-t border-outline-variant/60 text-xs text-outline">
              {spintaxTemplate.length} characters
            </div>
          </div>

          <div class="mt-4 flex gap-3">
            <button
              onclick={generateSpintaxPreview}
              class="bg-secondary text-white px-5 py-2.5 rounded-lg text-xs font-bold hover:brightness-115 active:scale-95 transition-all flex items-center gap-1.5 cursor-pointer focus-visible:ring-2 focus-visible:ring-secondary"
            >
              <span class="material-symbols-outlined text-sm" aria-hidden="true">casino</span>
              Randomize Preview
            </button>
            <button
              onclick={() => {
                spintaxTemplate = "Привет! {Увидел твой профиль|Наткнулась на твой блог}, {хочу предложить|есть классное предложение}...";
                generateSpintaxPreview();
              }}
              class="border border-outline-variant hover:bg-surface-container-high text-on-surface-variant px-4 py-2.5 rounded-lg text-xs font-semibold cursor-pointer"
            >
              Reset Default
            </button>
          </div>
        </div>

        <!-- Randomized Preview Area (fixed size container reservation for layout stability) -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
          <div class="p-4 border-b border-outline-variant bg-surface-container-low">
            <h4 class="font-bold text-on-surface">Live Message Preview Variation</h4>
          </div>
          <div class="p-6 bg-surface-container-lowest min-h-[120px] flex items-center justify-start">
            {#if randomizedPreview}
              <p class="text-sm text-on-surface whitespace-pre-wrap leading-relaxed italic border-l-4 border-l-secondary pl-4 py-1">
                "{randomizedPreview}"
              </p>
            {:else}
              <p class="text-xs text-on-surface-variant italic">Enter a spintax template above to view preview variations...</p>
            {/if}
          </div>
        </div>
      </section>

      <!-- Right Column: Spintax guide -->
      <aside class="lg:col-span-4 space-y-6">
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 shadow-sm">
          <h5 class="font-bold text-on-surface mb-3 text-sm uppercase tracking-wider">How Spintax Works</h5>
          <p class="text-xs text-on-surface-variant mb-4 leading-relaxed">
            Spintax allows you to send unique messages to every recipient automatically, drastically lowering spam scoring and maximizing outreach limits safely.
          </p>
          <div class="space-y-3">
            <div class="p-3 bg-surface-container-low rounded-lg">
              <span class="text-xs font-bold text-secondary uppercase block mb-1">Syntax Pattern</span>
              <code class="text-xs bg-white text-on-surface border border-outline-variant/40 px-1.5 py-0.5 rounded">
                &#123;Option 1|Option 2|Option 3&#125;
              </code>
            </div>
            <div class="p-3 bg-surface-container-low rounded-lg">
              <span class="text-xs font-bold text-secondary uppercase block mb-1">Example Template</span>
              <p class="text-xs text-on-surface italic">
                "&#123;Привет|Здравствуйте&#125;, &#123;нашел твой пост|увидел блог&#125;"
              </p>
            </div>
            <div class="p-3 bg-surface-container-low rounded-lg">
              <span class="text-xs font-bold text-green-700 uppercase block mb-1">Generated Sample Output</span>
              <p class="text-xs text-on-surface italic">"Здравствуйте, увидел блог"</p>
            </div>
          </div>
        </div>
      </aside>
    </div>
  {:else}
    <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-8 text-center" aria-live="polite">
      <span class="material-symbols-outlined text-4xl text-outline mb-2" aria-hidden="true">dashboard_customize</span>
      <p class="text-sm text-on-surface-variant italic">This tab is a placeholder. Please use 'Contacts Upload' or 'Spintax Editor' to inspect the JTBD feature workflows.</p>
    </div>
  {/if}
</main>

<!-- Bottom Navigation Bar (Visible on mobile/touch interfaces) -->
<nav class="fixed bottom-0 left-0 w-full flex justify-around items-center px-2 py-2 bg-surface border-t border-outline-variant md:hidden z-50 shadow-inner">
  <button
    onclick={() => currentTab = "campaigns"}
    class="flex flex-col items-center justify-center px-4 py-1 hover:text-secondary transition-transform scale-95 active:scale-90 cursor-pointer {currentTab === 'campaigns' ? 'text-secondary font-bold' : 'text-on-surface-variant'}"
  >
    <span class="material-symbols-outlined">analytics</span>
    <span class="text-[10px] font-semibold mt-0.5">Campaigns</span>
  </button>
  <button
    onclick={() => currentTab = "contacts"}
    class="flex flex-col items-center justify-center px-4 py-1 hover:text-secondary transition-transform scale-95 active:scale-90 cursor-pointer {currentTab === 'contacts' ? 'text-secondary font-bold bg-surface-container rounded-lg' : 'text-on-surface-variant'}"
  >
    <span class="material-symbols-outlined">group</span>
    <span class="text-[10px] font-semibold mt-0.5">Contacts</span>
  </button>
  <button
    onclick={() => currentTab = "spintax"}
    class="flex flex-col items-center justify-center px-4 py-1 hover:text-secondary transition-transform scale-95 active:scale-90 cursor-pointer {currentTab === 'spintax' ? 'text-secondary font-bold bg-surface-container rounded-lg' : 'text-on-surface-variant'}"
  >
    <span class="material-symbols-outlined">auto_awesome_motion</span>
    <span class="text-[10px] font-semibold mt-0.5">Spintax</span>
  </button>
  <button
    onclick={() => currentTab = "analytics"}
    class="flex flex-col items-center justify-center px-4 py-1 hover:text-secondary transition-transform scale-95 active:scale-90 cursor-pointer {currentTab === 'analytics' ? 'text-secondary font-bold' : 'text-on-surface-variant'}"
  >
    <span class="material-symbols-outlined">monitoring</span>
    <span class="text-[10px] font-semibold mt-0.5">Analytics</span>
  </button>
</nav>

<!-- Success/Toast Notification (Reserved layout and animated for high per-pixel visual quality) -->
<div
  id="toast"
  aria-live="assertive"
  class="fixed bottom-20 md:bottom-8 right-4 md:right-8 bg-inverse-surface text-inverse-on-surface px-6 py-4 rounded-xl shadow-2xl flex items-center gap-3 transition-all duration-500 z-[100] max-w-[90vw] md:max-w-md {showToast ? 'translate-y-0 opacity-100' : 'translate-y-32 opacity-0 pointer-events-none'}"
>
  <span class="material-symbols-outlined text-green-400" aria-hidden="true">check_circle</span>
  <div>
    <p class="text-sm font-bold text-white">{toastTitle}</p>
    <p class="text-xs text-on-surface-variant opacity-90 mt-0.5">{toastBody}</p>
  </div>
</div>
