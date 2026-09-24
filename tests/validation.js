const results = [];

function check(label, condition) {
  results.push(`${condition ? "PASS" : "FAIL"}: ${label}`);
}

const cases = [
  ["documento.pdf", true],
  ["DOCUMENTO.PDF", true],
  ["documento.PdF", true],
  ["documento con spazi.pdf", true],
  ["documento.v2.pdf", true],
  ["foto.png", false],
  ["documento.pdf.exe", false],
  ["documento", false],
  ["pdf", false],
  [".pdf", false],
  ["documento.pdf ", false],
  ["", false],
];

for (const [filename, expected] of cases) {
  check(`Estensione: ${JSON.stringify(filename)}`, hasPdfExtension(filename) === expected);
}

async function selectFile(filename, content = "test") {
  const transfer = new DataTransfer();
  if (filename) transfer.items.add(new File([content], filename));
  fileInput.files = transfer.files;
  await handleFileSelection();
}

async function runTests() {
  const originalFetch = window.fetch;
  const requests = [];
  let completeRequest;
  window.fetch = (url, options) => {
    requests.push({ url, options });
    return new Promise((resolve) => { completeRequest = resolve; });
  };

  try {
    check("Esito inizialmente vuoto", feedback.textContent === "");
    await selectFile("foto.png");
    check("Estensione errata non inviata", requests.length === 0 && feedback.dataset.state === "error");
    await selectFile("vuoto.pdf", "");
    check("File vuoto non inviato", requests.length === 0 && feedback.dataset.state === "error");
    await selectFile("grande.pdf", new Uint8Array(maxFileSize + 1));
    check("File troppo grande non inviato", requests.length === 0 && feedback.dataset.state === "error");
    const pending = selectFile("documento.pdf");
    check("Attesa senza falso successo", feedback.dataset.state === "pending" && selectButton.disabled);
    check("Contratto REST multipart", requests[0].url === "/api/files" && requests[0].options.method === "POST" && requests[0].options.body.get("file").name === "documento.pdf");
    await selectFile("duplicato.pdf");
    check("Invii simultanei bloccati", requests.length === 1);
    completeRequest({ ok: true, json: async () => ({ id: "stored.pdf" }) });
    await pending;
    check("Successo dopo risposta server", feedback.dataset.state === "success" && feedback.textContent === "File PDF caricato correttamente.");
    check("Pulsante ripristinato", !selectButton.disabled && !selectButton.hasAttribute("aria-busy"));
    await selectFile();
    check("Annullamento conserva esito", feedback.dataset.state === "success");
    const repeated = selectFile("documento.pdf");
    completeRequest({ ok: false, json: async () => ({ message: "Il contenuto non e' un PDF valido." }) });
    await repeated;
    check("Rifiuto server mostrato", feedback.dataset.state === "error" && feedback.textContent.includes("contenuto"));
    check("Selezione azzerata", fileInput.files.length === 0);
    window.fetch = async () => { throw new Error("Network failure"); };
    await selectFile("documento.pdf");
    check("Errore di rete e pulsante ripristinato", feedback.dataset.state === "error" && !selectButton.disabled);
    window.fetch = async () => ({ ok: false, json: async () => { throw new SyntaxError(); } });
    await selectFile("documento.pdf");
    check("Risposta non JSON gestita", feedback.dataset.state === "error" && !selectButton.disabled);
    window.fetch = async () => ({ ok: true, json: async () => ({ id: "new.pdf" }) });
    await selectFile("DOCUMENTO.PDF");
    check("Recupero dopo errore e maiuscole", feedback.dataset.state === "success");
  } catch (error) {
    check(`Errore inatteso: ${error.message}`, false);
  } finally {
    window.fetch = originalFetch;
    const failures = results.filter((result) => result.startsWith("FAIL"));
    document.querySelector("#results").textContent = `${results.join("\n")}\n\n${results.length - failures.length}/${results.length} test superati.`;
    document.body.dataset.testResult = failures.length ? "fail" : "pass";
  }
}

runTests();