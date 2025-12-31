package portal

import (
	"html/template"
	"net/http"
	"path/filepath"
)

// PortalHandler handles portal page rendering
type PortalHandler struct {
	templates *template.Template
}

// NewPortalHandler creates a new portal handler
func NewPortalHandler(templatesDir string) (*PortalHandler, error) {
	// Parse all templates
	tmpl, err := template.ParseGlob(filepath.Join(templatesDir, "*.html"))
	if err != nil {
		return nil, err
	}

	return &PortalHandler{
		templates: tmpl,
	}, nil
}

// PageData contains common data for all pages
type PageData struct {
	Title    string
	Subtitle string
	Active   string
}

// Dashboard renders the dashboard page
func (h *PortalHandler) Dashboard(w http.ResponseWriter, r *http.Request) {
	data := PageData{
		Title:    "Dashboard",
		Subtitle: "Overview of your vending machine performance",
		Active:   "dashboard",
	}

	if err := h.templates.ExecuteTemplate(w, "dashboard.html", data); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

// Products renders the products management page
func (h *PortalHandler) Products(w http.ResponseWriter, r *http.Request) {
	data := PageData{
		Title:    "Products",
		Subtitle: "Manage your product catalog",
		Active:   "products",
	}

	if err := h.templates.ExecuteTemplate(w, "products.html", data); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

// Transactions renders the transactions/payment log page
func (h *PortalHandler) Transactions(w http.ResponseWriter, r *http.Request) {
	data := PageData{
		Title:    "Transactions",
		Subtitle: "View and manage payment history",
		Active:   "transactions",
	}

	if err := h.templates.ExecuteTemplate(w, "transactions.html", data); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

// Inventory renders the inventory management page
func (h *PortalHandler) Inventory(w http.ResponseWriter, r *http.Request) {
	// Redirect to existing coils endpoint for now
	http.Redirect(w, r, "/portal/dashboard", http.StatusTemporaryRedirect)
}
