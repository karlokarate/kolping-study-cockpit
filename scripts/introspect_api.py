#!/usr/bin/env python3
"""Introspect the GraphQL API to discover available queries and types."""

import httpx

ENDPOINT = "https://app-kolping-prod-gateway.azurewebsites.net/graphql"


def introspect_queries():
    """Get all available root queries."""
    query = """
    {
        __schema {
            queryType {
                fields {
                    name
                    description
                    args { name type { name kind ofType { name } } }
                    type { name kind ofType { name kind ofType { name } } }
                }
            }
        }
    }
    """
    response = httpx.post(ENDPOINT, json={"query": query}, timeout=30)
    return response.json()


def introspect_type(type_name: str):
    """Get fields for a specific type."""
    query = f"""
    {{
        __type(name: "{type_name}") {{
            name
            fields {{
                name
                type {{ name kind ofType {{ name }} }}
            }}
        }}
    }}
    """
    response = httpx.post(ENDPOINT, json={"query": query}, timeout=30)
    return response.json()


def test_query(query_str: str):
    """Test a GraphQL query."""
    response = httpx.post(ENDPOINT, json={"query": query_str}, timeout=30)
    return response.status_code, response.json()


if __name__ == "__main__":
    # Key types to inspect for field names
    types_to_check = [
        "Semester",
        "Modul",
        "Studiengang",
        "Pruefung",
        "MatchModulStudent",
        "Student",
        "MyStudentData",
        "MyStudentGradeOverview",
    ]

    print("=== Type Field Names ===")
    for type_name in types_to_check:
        result = introspect_type(type_name)
        if result.get("data", {}).get("__type"):
            fields = result["data"]["__type"]["fields"]
            field_names = [f["name"] for f in fields]
            print(f"\n{type_name}:")
            print(f"  {field_names}")

    # Test simple queries
    print("\n=== Testing Queries ===")

    queries_to_test = [
        ("semesters", "{ semesters { id semesterName } }"),
        ("moduls", "{ moduls { id modulName } }"),
        ("studiengangs", "{ studiengangs { id } }"),
        ("pruefungs", "{ pruefungs { id } }"),
    ]

    for name, q in queries_to_test:
        status, result = test_query(q)
        has_data = bool(result.get("data", {}).get(name))
        has_errors = bool(result.get("errors"))
        print(
            f"  {name}: status={status}, has_data={has_data}, errors={result.get('errors', [])[:1]}"
        )
