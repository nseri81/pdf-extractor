const selectButton = document.querySelector("#select-file");
const fileInput = document.querySelector("#file-input");
const feedback = document.querySelector("#feedback");
const maxFileSize = 10 * 1024 * 1024;
let uploading = false;

function hasPdfExtension(filename) {
  return filename.length > 4 && /\.pdf$/i.test(filename);
}

selectButton.addEventListener("click", () => {
  fileInput.click();
});

async function handleFileSelection() {
  const file = fileInput.files[0];

  if (!file || uploading) return;
  fileInput.value = "";

  if (!hasPdfExtension(file.name)) {
    feedback.dataset.state = "error";
    feedback.textContent = "Formato non valido. Seleziona un file con estensione .pdf.";
    return;
  }

  if (file.size === 0 || file.size > maxFileSize) {
    feedback.dataset.state = "error";
    feedback.textContent = "Seleziona un PDF non vuoto di massimo 10 MB.";
    return;
  }

  uploading = true;
  selectButton.disabled = true;
  selectButton.setAttribute("aria-busy", "true");
  feedback.dataset.state = "pending";
  feedback.textContent = "Caricamento in corso...";

  try {
    const body = new FormData();
    body.append("file", file);
    const response = await fetch("/api/files", { method: "POST", body });
    const result = await response.json();

    if (!response.ok) {
      feedback.dataset.state = "error";
      feedback.textContent = result.message || "Caricamento non riuscito. Riprova.";
      return;
    }

    feedback.dataset.state = "success";
    feedback.textContent = "File PDF caricato correttamente.";
  } catch {
    feedback.dataset.state = "error";
    feedback.textContent = "Impossibile completare il caricamento. Verifica la connessione al server e riprova.";
  } finally {
    uploading = false;
    selectButton.disabled = false;
    selectButton.removeAttribute("aria-busy");
  }
}

fileInput.addEventListener("change", handleFileSelection);