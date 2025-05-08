let currentStudent = null;

// Base URL for the backend API
const API_URL = 'http://localhost:8080/student';

// Initialize the app on page load
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Page loaded, checking for persisted user...');
    const storedStudent = localStorage.getItem('currentStudent');
    console.log('Stored student in localStorage:', storedStudent);

    if (storedStudent) {
        try {
            currentStudent = JSON.parse(storedStudent);
            console.log('Parsed currentStudent:', currentStudent);

            console.log(`Verifying student with ID: ${currentStudent.id}`);
            const response = await fetch(`${API_URL}/verify?studentId=${currentStudent.id}`);
            console.log('Verification response status:', response.status);

            if (response.ok) {
                console.log('Verification successful, showing main section.');
                showMainSection();
            } else if (response.status === 401) {
                console.error('Student not found, logging out. Response text:', await response.text());
                localStorage.removeItem('currentStudent');
                currentStudent = null;
                alert('Your session has expired. Please log in again.');
                showLoginForm();
            } else {
                console.warn('Verification failed, but keeping user logged in. Response status:', response.status, 'Response text:', await response.text());
                showMainSection();
            }
        } catch (error) {
            console.warn('Network error during verification, keeping user logged in:', error.message);
            showMainSection();
        }
    } else {
        console.log('No stored student found, showing login form.');
        showLoginForm();
    }
});

// Clear the registration form
function clearRegisterForm() {
    const form = document.getElementById('register-form');
    form.reset();
    document.getElementById('name').value = '';
    document.getElementById('surname').value = '';
    document.getElementById('register-email').value = '';
    document.getElementById('register-password').value = '';
    document.getElementById('isGraduated').checked = false;
    document.getElementById('register-error').style.display = 'none';
}

// Show the register form
function showRegisterForm() {
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('register-form').style.display = 'block';
    document.getElementById('main-section').style.display = 'none';
    clearRegisterForm();
}

// Show the login form
function showLoginForm() {
    document.getElementById('register-form').style.display = 'none';
    document.getElementById('login-form').style.display = 'block';
    document.getElementById('main-section').style.display = 'none';
    document.getElementById('login-error').style.display = 'none';
    clearRegisterForm();
}

// Register a new student
async function register() {
    const name = document.getElementById('name').value;
    const surname = document.getElementById('surname').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const isGraduated = document.getElementById('isGraduated').checked;

    const student = { name, surname, email, isGraduated, password };

    try {
        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(student)
        });

        if (response.ok) {
            const data = await response.json();
            currentStudent = data;
            console.log('Saving currentStudent to localStorage:', currentStudent);
            localStorage.setItem('currentStudent', JSON.stringify(currentStudent));
            clearRegisterForm();
            showMainSection();
        } else {
            const error = await response.text();
            document.getElementById('register-error').textContent = error || 'Registration failed';
            document.getElementById('register-error').style.display = 'block';
        }
    } catch (error) {
        document.getElementById('register-error').textContent = 'Network error: ' + error.message;
        document.getElementById('register-error').style.display = 'block';
    }
}

// Login to the portal
async function login() {
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (response.ok) {
            const data = await response.json();
            if (data) {
                currentStudent = data;
                console.log('Saving currentStudent to localStorage:', currentStudent);
                localStorage.setItem('currentStudent', JSON.stringify(currentStudent));
                showMainSection();
            } else {
                document.getElementById('login-error').textContent = 'Login failed: Student not found';
                document.getElementById('login-error').style.display = 'block';
            }
        } else {
            const error = await response.text();
            document.getElementById('login-error').textContent = error || 'Login failed';
            document.getElementById('login-error').style.display = 'block';
        }
    } catch (error) {
        document.getElementById('login-error').textContent = 'Network error: ' + error.message;
        document.getElementById('login-error').style.display = 'block';
    }
}

// Update showMainSection to load invoices
async function showMainSection() {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('main-section').style.display = 'block';

    document.getElementById('student-name').textContent = currentStudent.name || 'Unknown';
    document.getElementById('student-id').textContent = currentStudent.id || 'Unknown';

    try {
        await loadCourses();
    } catch (error) {
        console.error('Failed to load courses:', error.message);
        const courseList = document.getElementById('course-list');
        courseList.innerHTML = '<li>Failed to load courses. Please try again later.</li>';
    }

    try {
        await viewEnrolments();
    } catch (error) {
        console.error('Failed to load enrolments:', error.message);
        const enrolmentList = document.getElementById('enrolment-list');
        enrolmentList.innerHTML = '<li>Failed to load enrolments. Please try again later.</li>';
    }

    try {
        await viewInvoices();
    } catch (error) {
        console.error('Failed to load invoices:', error.message);
        const invoiceList = document.getElementById('invoice-list');
        invoiceList.innerHTML = '<li>Failed to load invoices. Please try again later.</li>';
    }
}

// Load available courses
async function loadCourses() {
    try {
        const enrolmentsResponse = await fetch(`${API_URL}/enrolments?studentId=${currentStudent.id}`);
        let enrolledCourseIds = [];
        if (enrolmentsResponse.ok) {
            const enrolments = await enrolmentsResponse.json();
            enrolledCourseIds = enrolments.map(course => {
                try {
                    const parts = course.split(' - ');
                    if (parts.length < 2) return null;
                    const idPart = parts[0].split(': ');
                    if (idPart.length < 2 || idPart[0] !== 'Course ID') return null;
                    return idPart[1];
                } catch (error) {
                    console.error('Error extracting enrolled courseId:', error.message, 'Course:', course);
                    return null;
                }
            }).filter(id => id !== null);
        }

        const response = await fetch(`${API_URL}/courses`);
        if (response.ok) {
            const courses = await response.json();
            const courseList = document.getElementById('course-list');
            courseList.innerHTML = '';
            courses.forEach(course => {
                const li = document.createElement('li');
                const courseContainer = document.createElement('div');
                courseContainer.style.display = 'flex';
                courseContainer.style.alignItems = 'center';
                courseContainer.style.justifyContent = 'space-between';

                const courseText = document.createElement('span');
                courseText.textContent = `ID: ${course.id} - ${course.name} (${course.description})`;
                courseContainer.appendChild(courseText);

                const enrolButton = document.createElement('button');
                enrolButton.textContent = 'Enrol';
                enrolButton.style.backgroundColor = 'green';
                enrolButton.style.color = 'white';
                enrolButton.style.marginLeft = '10px';
                const isAlreadyEnrolled = enrolledCourseIds.includes(course.id.toString());
                enrolButton.disabled = isAlreadyEnrolled;
                enrolButton.style.opacity = isAlreadyEnrolled ? '0.5' : '1';
                enrolButton.onclick = () => enrolInCourse(course.id);
                courseContainer.appendChild(enrolButton);

                li.appendChild(courseContainer);
                courseList.appendChild(li);
            });
        } else {
            alert('Failed to load courses');
        }
    } catch (error) {
        alert('Network error while loading courses: ' + error.message);
    }
}

// Enrol in a course
async function enrolInCourse(courseId) {
    try {
        const enrolmentsResponse = await fetch(`${API_URL}/enrolments?studentId=${currentStudent.id}`);
        if (enrolmentsResponse.ok) {
            const enrolments = await enrolmentsResponse.json();
            const isAlreadyEnrolled = enrolments.some(course => {
                let enrolledCourseId;
                try {
                    const parts = course.split(' - ');
                    if (parts.length < 2) throw new Error('Invalid course format');
                    const idPart = parts[0].split(': ');
                    if (idPart.length < 2 || idPart[0] !== 'Course ID') throw new Error('Invalid course ID format');
                    enrolledCourseId = idPart[1];
                } catch (error) {
                    console.error('Error extracting enrolled courseId:', error.message, 'Course:', course);
                    return false;
                }
                return enrolledCourseId === courseId.toString();
            });

            if (isAlreadyEnrolled) {
                alert('You are already enrolled in this course.');
                return;
            }
        } else {
            alert('Failed to check current enrolments. Please try again.');
            return;
        }

        const response = await fetch(`${API_URL}/enrol?studentId=${currentStudent.id}&courseId=${courseId}`, {
            method: 'POST'
        });

        if (response.ok) {
            alert('Enrolled successfully');
            await viewEnrolments();
            await loadCourses();
        } else {
            const error = await response.text();
            alert(error || 'Enrolment failed');
        }
    } catch (error) {
        alert('Network error while enrolling: ' + error.message);
    }
}

// View enrolments
async function viewEnrolments() {
    try {
        const response = await fetch(`${API_URL}/enrolments?studentId=${currentStudent.id}`);
        if (response.ok) {
            const enrolments = await response.json();
            const enrolmentList = document.getElementById('enrolment-list');
            enrolmentList.innerHTML = '';
            enrolments.forEach(course => {
                const li = document.createElement('li');
                const courseContainer = document.createElement('div');
                courseContainer.style.display = 'flex';
                courseContainer.style.alignItems = 'center';
                courseContainer.style.justifyContent = 'space-between';

                const courseText = document.createElement('span');
                courseText.textContent = course;
                courseContainer.appendChild(courseText);

                let courseId;
                try {
                    const parts = course.split(' - ');
                    if (parts.length < 2) throw new Error('Invalid course format');
                    const idPart = parts[0].split(': ');
                    if (idPart.length < 2 || idPart[0] !== 'Course ID') throw new Error('Invalid course ID format');
                    courseId = idPart[1];
                    if (!courseId || isNaN(courseId)) throw new Error('Course ID is not a number');
                } catch (error) {
                    console.error('Error extracting courseId:', error.message, 'Course:', course);
                    courseId = null;
                }

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Delete';
                deleteButton.style.backgroundColor = 'red';
                deleteButton.style.color = 'white';
                deleteButton.style.marginLeft = '10px';
                deleteButton.disabled = !courseId;
                deleteButton.onclick = () => courseId && deleteEnrolment(courseId);
                courseContainer.appendChild(deleteButton);

                li.appendChild(courseContainer);
                enrolmentList.appendChild(li);
            });
        } else {
            alert('Failed to load enrolments');
        }
    } catch (error) {
        alert('Network error while loading enrolments: ' + error.message);
    }
}

// Delete an individual enrolment
async function deleteEnrolment(courseId) {
    if (!confirm(`Are you sure you want to unenroll from course ID ${courseId}?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_URL}/unenrol?studentId=${currentStudent.id}&courseId=${courseId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('Unenrolled successfully');
            await viewEnrolments();
            await loadCourses();
        } else {
            const error = await response.text();
            alert('Failed to unenroll: ' + error);
        }
    } catch (error) {
        alert('Network error while unenrolling: ' + error.message);
    }
}

// Load invoices
async function viewInvoices() {
    try {
        const status = document.getElementById('invoice-status').value;
        const type = document.getElementById('invoice-type').value;
        let url = `${API_URL}/${currentStudent.id}/invoices`;
        if (status || type) {
            url += '?';
            if (status) url += `status=${status}`;
            if (status && type) url += '&';
            if (type) url += `type=${type}`;
        }

        const response = await fetch(url);
        if (response.ok) {
            const invoices = await response.json();
            const invoiceList = document.getElementById('invoice-list');
            invoiceList.innerHTML = '';
            if (invoices.length === 0) {
                invoiceList.innerHTML = '<li>No invoices found.</li>';
                return;
            }
            invoices.forEach(invoice => {
                const li = document.createElement('li');
                li.textContent = `Invoice ID: ${invoice.id} | Type: ${invoice.type} | Amount: £${invoice.amount} | Status: ${invoice.status} | Description: ${invoice.description} | Due: ${invoice.dueDate}`;
                invoiceList.appendChild(li);
            });
        } else {
            alert('Failed to load invoices');
        }
    } catch (error) {
        alert('Network error while loading invoices: ' + error.message);
    }
}

// Update student profile
async function updateProfile() {
    const name = document.getElementById('update-name').value;
    const surname = document.getElementById('update-surname').value;

    try {
        const response = await fetch(`${API_URL}/${currentStudent.id}?name=${name}&surname=${surname}`, {
            method: 'PUT'
        });

        if (response.ok) {
            const updatedStudent = await response.json();
            currentStudent = updatedStudent;
            localStorage.setItem('currentStudent', JSON.stringify(currentStudent));
            document.getElementById('student-name').textContent = currentStudent.name;
            alert('Profile updated successfully');
        } else {
            const error = await response.text();
            alert('Profile update failed: ' + error);
        }
    } catch (error) {
        alert('Network error while updating profile: ' + error.message);
    }
}

// Check graduation eligibility
async function checkGraduation() {
    try {
        const response = await fetch(`${API_URL}/graduate/${currentStudent.id}`);
        if (response.ok) {
            const eligible = await response.json();
            document.getElementById('graduation-status').textContent = eligible
                ? 'You are eligible to graduate!'
                : 'You are not eligible to graduate due to outstanding invoices.';
        } else {
            const error = await response.text();
            alert('Failed to check graduation eligibility: ' + error);
        }
    } catch (error) {
        alert('Network error while checking graduation eligibility: ' + error.message);
    }
}

// Delete the student account
async function deleteAccount() {
    if (!confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch(`${API_URL}/delete/${currentStudent.id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('Account deleted successfully');
            logout();
        } else {
            const error = await response.text();
            alert('Failed to delete account: ' + error);
        }
    } catch (error) {
        alert('Network error while deleting account: ' + error.message);
    }
}

// Logout
function logout() {
    console.log('Logging out user, clearing localStorage...');
    localStorage.removeItem('currentStudent');
    currentStudent = null;
    document.getElementById('auth-section').style.display = 'block';
    document.getElementById('main-section').style.display = 'none';
    document.getElementById('register-form').style.display = 'none';
    document.getElementById('login-form').style.display = 'block';
    document.getElementById('login-email').value = '';
    document.getElementById('login-password').value = '';
    document.getElementById('login-error').style.display = 'none';
    clearRegisterForm();
}