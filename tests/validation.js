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

function selectFile(filename) {
  const transfer = new DataTransfer();
  if (filename) transfer.items.add(new File(["test"], filename));
  fileInput.files = transfer.files;
  fileInput.dispatchEvent(new Event("change", { bubbles: true }));
}

check("Esito inizialmente vuoto", feedback.textContent === "");
selectFile("documento.pdf");
check("PDF accettato senza MIME", feedback.dataset.state === "success");
check("Messaggio positivo", feedback.textContent === "File PDF selezionato correttamente.");
check("Selezione azzerata senza conservare il file", fileInput.files.length === 0);
selectFile("documento.pdf");
check("Stesso file riselezionabile", feedback.dataset.state === "success");
selectFile("foto.png");
check("File non PDF rifiutato", feedback.dataset.state === "error");
check("Messaggio di errore", feedback.textContent.includes("Formato non valido"));
selectFile();
check("Annullamento conserva esito", feedback.dataset.state === "error");
selectFile("DOCUMENTO.PDF");
check("Recupero dopo errore", feedback.dataset.state === "success");

const failures = results.filter((result) => result.startsWith("FAIL"));
document.querySelector("#results").textContent = `${results.join("\n")}\n\n${results.length - failures.length}/${results.length} test superati.`;
document.body.dataset.testResult = failures.length ? "fail" : "pass";