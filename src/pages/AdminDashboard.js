import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Eye, Pencil, Trash2, Plus } from 'lucide-react';
import { getEmployeesPage, getEmployeeById, deleteEmployee } from '../services/employeeService';
import AdminLayout from '../components/AdminLayout';
import '../styles/tailwind.css';

function AdminDashboard({ userName, onLogout }) {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalEmployees, setTotalEmployees] = useState(null);
  const [pageSize, setPageSize] = useState(5);
  const RECORDS_PER_PAGE = 5;
  const navigate = useNavigate();
  const latestRequestIdRef = useRef(0);

  useEffect(() => {
    loadEmployees(currentPage, searchQuery.trim());
  }, [currentPage, searchQuery]);

  // A short all-digit query (e.g. "5", "23") is treated as an employee ID lookup
  // rather than a general text search, so it only ever returns the exact ID match
  // instead of the backend's fuzzy "search" endpoint pulling in unrelated records
  // whose phone/other fields merely contain that substring.
  const isEmployeeIdQuery = (query) => /^\d{1,6}$/.test(query);

  const loadEmployees = async (page, query) => {
    const requestId = ++latestRequestIdRef.current;
    setLoading(true);
    try {
      if (isEmployeeIdQuery(query)) {
        let match = null;
        try {
          match = await getEmployeeById(Number(query));
        } catch (lookupErr) {
          match = null;
        }
        if (requestId !== latestRequestIdRef.current) return;
        setEmployees(match ? [match] : []);
        setTotalEmployees(match ? 1 : 0);
        setPageSize(RECORDS_PER_PAGE);
        return;
      }

      const { employees: data, total, pageSize: responsePageSize } = await getEmployeesPage({
        page,
        size: RECORDS_PER_PAGE,
        searchQuery: query,
      });
      if (requestId !== latestRequestIdRef.current) return;
      setEmployees(data);
      setTotalEmployees(total);
      setPageSize(responsePageSize || RECORDS_PER_PAGE);
    } catch (err) {
      if (requestId !== latestRequestIdRef.current) return;
      console.error('Failed to load employees:', err);
    } finally {
      if (requestId === latestRequestIdRef.current) setLoading(false);
    }
  };

  const handleEdit = (employee) => {
    navigate(`/admin/employee/${employee.id}?edit=true`);
  };

  const handleDelete = async (employeeId) => {
    if (window.confirm('Are you sure you want to delete this employee?')) {
      try {
        await deleteEmployee(employeeId);
        setEmployees(employees.filter((emp) => emp.id !== employeeId));
        alert('Employee deleted successfully');
      } catch (err) {
        alert('Failed to delete employee');
      }
    }
  };

  const totalPages = totalEmployees !== null ? Math.max(1, Math.ceil(totalEmployees / pageSize)) : null;
  const isNextDisabled = totalPages ? currentPage >= totalPages : employees.length < pageSize;

  const handlePreviousPage = () => {
    setCurrentPage((prev) => Math.max(1, prev - 1));
  };

  const handleNextPage = () => {
    if (!isNextDisabled) {
      setCurrentPage((prev) => prev + 1);
    }
  };

  const handleSearchChange = (value) => {
    setSearchQuery(value);
    setCurrentPage(1);
  };

  return (
    <AdminLayout userName={userName} onLogout={onLogout} activeItem="dashboard" title="Employee Details" subtitle="View and manage all employee records in one place.">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-1 items-center gap-3">
            <h2 className="text-base font-semibold text-foreground">All Employees</h2>
            <div className="relative w-full max-w-sm">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                className="h-9 w-full rounded-lg border border-border bg-white pl-9 pr-3 text-sm outline-none focus:border-client focus:ring-2 focus:ring-client/30"
                placeholder="Search by first name, last name, email, phone, or designation"
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
              />
            </div>
          </div>
          <button
            onClick={() => navigate('/admin/employee/new')}
            className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-client px-3 text-sm font-medium text-client-foreground hover:bg-client/90"
          >
            <Plus className="size-4" />
            Create Employee
          </button>
        </div>

        {loading ? (
          <p className="text-sm text-muted-foreground">Loading employees...</p>
        ) : employees.length === 0 ? (
          <p className="text-sm text-muted-foreground">No employees found.</p>
        ) : (
          <>
            <div className="overflow-hidden rounded-xl border border-border/80 bg-card shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left" style={{ minWidth: 900 }}>
                  <thead>
                    <tr className="border-b border-border/80 bg-muted/40">
                      {['ID', 'First Name', 'Last Name', 'Email', 'Phone', 'Designation', 'Department', 'Status', 'Actions'].map(
                        (col) => (
                          <th
                            key={col}
                            className="h-11 whitespace-nowrap px-4 text-[11px] font-semibold uppercase tracking-[0.06em] text-muted-foreground"
                          >
                            {col}
                          </th>
                        )
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {employees.map((employee) => (
                      <tr key={employee.id} className="border-b border-border/60 last:border-0 hover:bg-muted/30">
                        <td className="px-4 py-3 text-sm text-foreground">{employee.employeeId || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-foreground">{employee.firstName || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-foreground">{employee.lastName || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">{employee.email}</td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">{employee.phone || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">{employee.designation || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">{employee.department || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">{employee.employeeStatus || 'Active'}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-1">
                            <button
                              title="View"
                              onClick={() => navigate(`/admin/employee/${employee.id}`)}
                              className="rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                            >
                              <Eye className="size-4" />
                            </button>
                            <button
                              title="Edit"
                              onClick={() => handleEdit(employee)}
                              className="rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                            >
                              <Pencil className="size-4" />
                            </button>
                            <button
                              title="Delete"
                              onClick={() => handleDelete(employee.id)}
                              className="rounded-md p-1.5 text-red-600 hover:bg-red-50"
                            >
                              <Trash2 className="size-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={handlePreviousPage}
                disabled={currentPage === 1}
                className="h-8 rounded-lg border border-border bg-white px-3 text-sm font-medium text-foreground hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-sm text-muted-foreground">
                Page {currentPage}{totalPages ? ` of ${totalPages}` : ''}
              </span>
              <button
                type="button"
                onClick={handleNextPage}
                disabled={isNextDisabled}
                className="h-8 rounded-lg border border-border bg-white px-3 text-sm font-medium text-foreground hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  );
}

export default AdminDashboard;
